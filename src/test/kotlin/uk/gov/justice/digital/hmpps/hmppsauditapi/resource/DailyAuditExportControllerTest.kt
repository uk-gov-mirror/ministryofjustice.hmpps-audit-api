package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.StaffAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DailyAuditExportControllerTest(
  @Autowired private val staffAuditRepository: StaffAuditRepository,
  @Autowired private val s3Client: S3Client,
  @Value("\${aws.s3.auditBucketName}") private val auditBucketName: String,
) : IntegrationTest() {

  private val exportDate = LocalDate.of(2025, 3, 10)
  private val expectedKey = "daily/year=2025/month=3/day=10/audit.parquet"

  @TestInstance(PER_CLASS)
  @Nested
  inner class Security {
    @Test
    fun `requires a valid authentication token`() {
      webTestClient.post().uri("/audit/STAFF/daily-export/$exportDate")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires the correct role`() {
      webTestClient.post().uri("/audit/STAFF/daily-export/$exportDate")
        .headers(setAuthorisation(roles = listOf("ROLE_SOMETHING_ELSE")))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @TestInstance(PER_CLASS)
  @Nested
  inner class Validation {
    @ParameterizedTest
    @EnumSource(value = AuditEventType::class, names = ["PRISONER", "PERSON_ON_PROBATION"])
    fun `rejects audit types other than staff`(auditEventType: AuditEventType) {
      webTestClient.post().uri("/audit/$auditEventType/daily-export/$exportDate")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.developerMessage").isEqualTo("Daily export is not supported for $auditEventType")
    }

    @Test
    fun `rejects today`() {
      webTestClient.post().uri("/audit/STAFF/daily-export/${LocalDate.now(ZoneOffset.UTC)}")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.developerMessage").isEqualTo("Date must be before today (UTC)")
    }

    @Test
    fun `rejects future dates`() {
      webTestClient.post().uri("/audit/STAFF/daily-export/${LocalDate.now(ZoneOffset.UTC).plusDays(1)}")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @ParameterizedTest
    @ValueSource(strings = ["2025-13-01", "not-a-date"])
    fun `rejects invalid dates`(date: String) {
      webTestClient.post().uri("/audit/STAFF/daily-export/$date")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @TestInstance(PER_CLASS)
  @Nested
  inner class HappyPath {
    private val dayStart = exportDate.atStartOfDay(ZoneOffset.UTC).toInstant()
    private val dayEnd = exportDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

    @BeforeAll
    fun setUp() {
      staffAuditRepository.deleteAll()
      staffAuditRepository.saveAll(
        listOf(
          staffEvent("previous day", dayStart.minusNanos(1_000)),
          staffEvent("second", dayStart.plusSeconds(60)),
          staffEvent("first", dayStart),
          staffEvent("last", dayEnd.minusNanos(1_000)),
          staffEvent("next day", dayEnd),
        ),
      )
    }

    @AfterAll
    fun tearDown() {
      staffAuditRepository.deleteAll()
      s3Client.deleteObject { it.bucket(auditBucketName).key(expectedKey) }
    }

    @Test
    fun `exports only the staff events for the UTC day into a single ordered parquet file`() {
      webTestClient.post().uri("/audit/STAFF/daily-export/$exportDate")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.auditEventType").isEqualTo("STAFF")
        .jsonPath("$.date").isEqualTo("2025-03-10")
        .jsonPath("$.eventCount").isEqualTo(3)
        .jsonPath("$.s3Key").isEqualTo(expectedKey)
        .jsonPath("$.fileSizeBytes").value<Int> { assertThat(it).isPositive() }

      val records = readParquet(auditBucketName, expectedKey)
      assertThat(records.map { it.get("what").toString() }).containsExactly("first", "second", "last")
      assertThat(records.first().get("who").toString()).isEqualTo("someone")
      assertThat(records.first().get("when").toString()).isEqualTo(dayStart.toString())
    }

    @Test
    fun `does not upload a file for a day with no events`() {
      val emptyDate = exportDate.minusDays(5)
      webTestClient.post().uri("/audit/STAFF/daily-export/$emptyDate")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.eventCount").isEqualTo(0)
        .jsonPath("$.s3Key").doesNotExist()
        .jsonPath("$.fileSizeBytes").isEqualTo(0)

      val emptyKey = "daily/year=2025/month=3/day=5/audit.parquet"
      val result = runCatching { s3Client.headObject(HeadObjectRequest.builder().bucket(auditBucketName).key(emptyKey).build()) }
      assertThat(result.exceptionOrNull()).isInstanceOf(NoSuchKeyException::class.java)
    }
  }

  private fun staffEvent(what: String, `when`: Instant) = StaffAuditEvent(what = what, `when` = `when`, who = "someone", service = "a-service")

  private fun readParquet(bucket: String, key: String): List<GenericRecord> {
    val file = Files.createTempFile("daily-export-test", ".parquet")
    try {
      s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build()).use { Files.write(file, it.readAllBytes()) }
      return AvroParquetReader.builder<GenericRecord>(Path(file.toUri())).build().use { reader ->
        generateSequence { reader.read() }.toList()
      }
    } finally {
      Files.deleteIfExists(file)
    }
  }
}

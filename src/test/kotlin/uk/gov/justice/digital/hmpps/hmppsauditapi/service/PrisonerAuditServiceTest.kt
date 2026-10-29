package uk.gov.justice.digital.hmpps.hmppsauditapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.then
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PrisonerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.PrisonerAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toPrisonerAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.AuditDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditAthenaClient
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditS3Client
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.PrisonerAuditService
import java.time.Instant
import java.util.UUID

class PrisonerAuditServiceTest {
  private val telemetryClient: TelemetryClient = mock()
  private val prisonerAuditRepository: PrisonerAuditRepository = mock()
  private val auditS3Client: AuditS3Client = mock()
  private val auditAthenaClient: AuditAthenaClient = mock()
  private val athenaPropertiesFactory: AthenaPropertiesFactory = mock()
  private val saveToS3Bucket = false
  private var auditService =
    PrisonerAuditService(
      telemetryClient,
      prisonerAuditRepository,
      auditS3Client,
      auditAthenaClient,
      athenaPropertiesFactory,
      saveToS3Bucket,
      auditEventDetails = false,
    )

  @Nested
  inner class FindAuditEvents {

    @Test
    fun `find all audit events`() {
      val listOfAudits = listOf(
        PrisonerAuditEvent(
          UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
          "MINIMUM_FIELDS_EVENT",
          Instant.parse("2021-04-04T17:17:30Z"),
        ),
        PrisonerAuditEvent(
          UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
          "COURT_REGISTER_BUILDING_UPDATE",
          Instant.parse("2021-04-03T10:15:30Z"),
          "badea6d876c62e2f5264c94c7b50875e",
          "1adea6d876c62e2f5264c94c7b508755",
          "PERSON",
          "3rdea6d876c62e2f5264c94c7b508548",
          "bobby.beans",
          "hmpps-launchpad-home-ui",
          "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}",
        ),
        PrisonerAuditEvent(
          UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
          "OFFENDER_DELETED",
          Instant.parse("2021-04-01T15:15:30Z"),
          "cadea6d876c62e2f5264c94c7b50875e",
          "4adea6d876c62e2f5264c94c7b50875e",
          "PERSON",
          "3rdea6d876c62e2f5264c94c7b508548",
          "bobby.beans",
          "hmpps-launchpad-home-ui",
          "{\"offenderId\": \"97\"}",
        ),
        PrisonerAuditEvent(
          UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
          "OFFENDER_DELETED",
          Instant.parse("2020-12-31T08:11:30Z"),
          "dadea6d876c62e2f5264c94c7b50875e",
          "5adea6d876c62e2f5264c94c7b50875e",
          "PERSON",
          "3rdea6d876c62e2f5264c94c7b508548",
          "freddy.frog",
          "hmpps-launchpad-home-ui",
          "{\"offenderId\": \"98\"}",
        ),
      )
      whenever(prisonerAuditRepository.findAll(any<Sort>())).thenReturn(
        listOfAudits,
      )

      val audits = auditService.findAll()
      assertThat(audits).isEqualTo(
        listOf(
          AuditDto(
            UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
            "MINIMUM_FIELDS_EVENT",
            Instant.parse("2021-04-04T17:17:30Z"),
            null,
            null,
            "NOT_APPLICABLE",
            null,
            null,
            null,
            null,
          ),
          AuditDto(
            UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
            "COURT_REGISTER_BUILDING_UPDATE",
            Instant.parse("2021-04-03T10:15:30Z"),
            "badea6d876c62e2f5264c94c7b50875e",
            "1adea6d876c62e2f5264c94c7b508755",
            "PERSON",
            "3rdea6d876c62e2f5264c94c7b508548",
            "bobby.beans",
            "hmpps-launchpad-home-ui",
            "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}",
          ),
          AuditDto(
            UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
            "OFFENDER_DELETED",
            Instant.parse("2021-04-01T15:15:30Z"),
            "cadea6d876c62e2f5264c94c7b50875e",
            "4adea6d876c62e2f5264c94c7b50875e",
            "PERSON",
            "3rdea6d876c62e2f5264c94c7b508548",
            "bobby.beans",
            "hmpps-launchpad-home-ui",
            "{\"offenderId\": \"97\"}",
          ),
          AuditDto(
            UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
            "OFFENDER_DELETED",
            Instant.parse("2020-12-31T08:11:30Z"),
            "dadea6d876c62e2f5264c94c7b50875e",
            "5adea6d876c62e2f5264c94c7b50875e",
            "PERSON",
            "3rdea6d876c62e2f5264c94c7b508548",
            "freddy.frog",
            "hmpps-launchpad-home-ui",
            "{\"offenderId\": \"98\"}",
          ),
        ),
      )
    }

    @Nested
    inner class FindPagedAuditEvents {

      @Test
      fun `find all paged audit events`() {
        val listOfAudits = PageImpl(
          listOf(
            PrisonerAuditEvent(
              UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
              "MINIMUM_FIELDS_EVENT",
              Instant.parse("2021-04-04T17:17:30Z"),
            ),
            PrisonerAuditEvent(
              UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
              "COURT_REGISTER_BUILDING_UPDATE",
              Instant.parse("2021-04-03T10:15:30Z"),
              "badea6d876c62e2f5264c94c7b50875e",
              "serea6d876c62e2f5264c94c7b5083et",
              "CASE_NOTE",
              "3rdea6d876c62e2f5264c94c7b508548",
              "bobby.beans",
              "hmpps-launchpad-home-ui",
              "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}",
            ),
            PrisonerAuditEvent(
              UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
              "OFFENDER_DELETED",
              Instant.parse("2021-04-01T15:15:30Z"),
              "cadea6d876c62e2f5264c94c7b50875e",
              "reqea6d876c62e2f5264c94c7b50843h",
              "PERSON",
              "3rdea6d876c62e2f5264c94c7b508548",
              "bobby.beans",
              "hmpps-launchpad-home-ui",
              "{\"offenderId\": \"97\"}",
            ),
            PrisonerAuditEvent(
              UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
              "OFFENDER_DELETED",
              Instant.parse("2020-12-31T08:11:30Z"),
              "dadea6d876c62e2f5264c94c7b50875e",
              "zadea6d876c62e2f5264c94c7b508752",
              "CASE_NOTE",
              "3rdea6d876c62e2f5264c94c7b508548",
              "freddy.frog",
              "hmpps-launchpad-home-ui",
              "{\"offenderId\": \"98\"}",
            ),
          ),
        )
        whenever(
          prisonerAuditRepository.findPage(
            any(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
          ),
        ).thenReturn(
          listOfAudits,
        )

        val auditFilterDto = AuditFilterDto(
          Instant.parse("2021-04-04T17:17:30Z"),
          Instant.parse("2023-01-03T17:17:30Z"),
          "hmpps-launchpad-home-ui",
          "Another Event",
          "Hola T",
        )

        val audits = auditService.findPage(Pageable.unpaged(), auditFilterDto)
        assertThat(audits).isEqualTo(
          PageImpl(
            listOf(
              AuditDto(
                UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
                "MINIMUM_FIELDS_EVENT",
                Instant.parse("2021-04-04T17:17:30Z"),
                null,
                null,
                "NOT_APPLICABLE",
                null,
                null,
                null,
                null,
              ),
              AuditDto(
                UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
                "COURT_REGISTER_BUILDING_UPDATE",
                Instant.parse("2021-04-03T10:15:30Z"),
                "badea6d876c62e2f5264c94c7b50875e",
                "serea6d876c62e2f5264c94c7b5083et",
                "CASE_NOTE",
                "3rdea6d876c62e2f5264c94c7b508548",
                "bobby.beans",
                "hmpps-launchpad-home-ui",
                "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}",
              ),
              AuditDto(
                UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
                "OFFENDER_DELETED",
                Instant.parse("2021-04-01T15:15:30Z"),
                "cadea6d876c62e2f5264c94c7b50875e",
                "reqea6d876c62e2f5264c94c7b50843h",
                "PERSON",
                "3rdea6d876c62e2f5264c94c7b508548",
                "bobby.beans",
                "hmpps-launchpad-home-ui",
                "{\"offenderId\": \"97\"}",
              ),
              AuditDto(
                UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
                "OFFENDER_DELETED",
                Instant.parse("2020-12-31T08:11:30Z"),
                "dadea6d876c62e2f5264c94c7b50875e",
                "zadea6d876c62e2f5264c94c7b508752",
                "CASE_NOTE",
                "3rdea6d876c62e2f5264c94c7b508548",
                "freddy.frog",
                "hmpps-launchpad-home-ui",
                "{\"offenderId\": \"98\"}",
              ),
            ),
          ),
        )
      }
    }
  }

  @Nested
  inner class FindFilteredAuditEvents {

    @Test
    fun `find all filtered audit events`() {
      val listOfAudits = PageImpl(
        listOf(
          PrisonerAuditEvent(
            UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
            "OFFENDER_DELETED",
            Instant.parse("2020-12-31T08:11:30Z"),
            "dadea6d876c62e2f5264c94c7b50875e",
            "fedea6d876c62e2f5264c94c7b50873w",
            "PERSON",
            "3rdea6d876c62e2f5264c94c7b508548",
            "freddy.frog",
            "hmpps-launchpad-home-ui",
            "{\"offenderId\": \"98\"}",
          ),
        ),
      )
      whenever(
        prisonerAuditRepository.findPage(
          any(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
        ),
      ).thenReturn(
        listOfAudits,
      )

      val startDate = Instant.parse("2021-04-04T17:17:30Z")
      val endDate = Instant.parse("2023-01-03T17:17:30Z")
      val pageDetails = Pageable.unpaged()

      val auditFilterDto = AuditFilterDto(
        startDate,
        endDate,
        "hmpps-launchpad-home-ui",
        "fedea6d876c62e2f5264c94c7b50873w",
        "PERSON",
        "3rdea6d876c62e2f5264c94c7b508548",
        "Hola T",
        "Another Event",
      )

      val audits = auditService.findPage(pageDetails, auditFilterDto)

      assertThat(audits).isEqualTo(
        PageImpl(
          listOf(
            AuditDto(
              UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
              "OFFENDER_DELETED",
              Instant.parse("2020-12-31T08:11:30Z"),
              "dadea6d876c62e2f5264c94c7b50875e",
              "fedea6d876c62e2f5264c94c7b50873w",
              "PERSON",
              "3rdea6d876c62e2f5264c94c7b508548",
              "freddy.frog",
              "hmpps-launchpad-home-ui",
              "{\"offenderId\": \"98\"}",
            ),
          ),
        ),
      )

      verify(prisonerAuditRepository).findPage(
        pageDetails,
        startDate,
        endDate,
        "hmpps-launchpad-home-ui",
        "fedea6d876c62e2f5264c94c7b50873w",
        "PERSON",
        "3rdea6d876c62e2f5264c94c7b508548",
        "Another Event",
        "Hola T",
      )
    }
  }

  @Nested
  inner class SaveAuditEvent {

    val auditEvent = AuditEvent(
      UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
      "homepage views",
      Instant.parse("2020-12-31T08:11:30Z"),
      "dadea6d876c62e2f5264c94c7b50875e",
      "fedea6d876c62e2f5264c94c7b50873w",
      "PERSON",
      "3rdea6d876c62e2f5264c94c7b508548",
      "freddy.frog",
      "hmpps-launchpad-ui",
      "{\"offenderId\": \"98\"}",
    )

    @Test
    fun `save audit event to database when saveToS3Bucket is false`() {
      auditService = PrisonerAuditService(telemetryClient, prisonerAuditRepository, auditS3Client, auditAthenaClient, athenaPropertiesFactory, false, auditEventDetails = false)

      auditService.saveAuditEvent(auditEvent)

      then(auditS3Client).shouldHaveNoInteractions()
      then(auditAthenaClient).shouldHaveNoInteractions()
      then(prisonerAuditRepository).should().save(auditEvent.toPrisonerAuditEvent())
    }

    @Test
    fun `save audit event to S3 bucket when saveToS3Bucket is true`() {
      whenever(athenaPropertiesFactory.getProperties(AuditEventType.PRISONER)).thenReturn(athenaProperties)
      auditService = PrisonerAuditService(telemetryClient, prisonerAuditRepository, auditS3Client, auditAthenaClient, athenaPropertiesFactory, true, auditEventDetails = false)

      auditService.saveAuditEvent(auditEvent)

      then(auditS3Client).should().save(auditEvent, athenaProperties.s3BucketName)
      then(auditAthenaClient).should().addPartitionForEvent(auditEvent, athenaProperties)
      then(prisonerAuditRepository).should().save(auditEvent.toPrisonerAuditEvent())
    }

    private val telemetryProperties = mapOf(
      "what" to "homepage views",
      "when" to "2020-12-31T08:11:30Z",
      "operationId" to "dadea6d876c62e2f5264c94c7b50875e",
      "subjectId" to "fedea6d876c62e2f5264c94c7b50873w",
      "subjectType" to "PERSON",
      "correlationId" to "3rdea6d876c62e2f5264c94c7b508548",
      "who" to "freddy.frog",
      "service" to "hmpps-launchpad-ui",
    )

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `include telemetry details only when auditEventDetails is true`(auditEventDetails: Boolean) {
      auditService = PrisonerAuditService(
        telemetryClient,
        prisonerAuditRepository,
        auditS3Client,
        auditAthenaClient,
        athenaPropertiesFactory,
        saveToS3Bucket = false,
        auditEventDetails = auditEventDetails,
      )

      auditService.saveAuditEvent(auditEvent)

      val expectedProperties = if (auditEventDetails) {
        telemetryProperties + ("details" to "{\"offenderId\": \"98\"}")
      } else {
        telemetryProperties
      }
      then(telemetryClient).should().trackEvent(AuditEventType.PRISONER.description, expectedProperties, null)
      then(prisonerAuditRepository).should().save(auditEvent.toPrisonerAuditEvent())
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `omit null telemetry details regardless of auditEventDetails`(auditEventDetails: Boolean) {
      auditService = PrisonerAuditService(
        telemetryClient,
        prisonerAuditRepository,
        auditS3Client,
        auditAthenaClient,
        athenaPropertiesFactory,
        saveToS3Bucket = false,
        auditEventDetails = auditEventDetails,
      )
      val eventWithoutDetails = auditEvent.copy(details = null)

      auditService.saveAuditEvent(eventWithoutDetails)

      then(telemetryClient).should().trackEvent(AuditEventType.PRISONER.description, telemetryProperties, null)
      then(prisonerAuditRepository).should().save(eventWithoutDetails.toPrisonerAuditEvent())
    }
  }

  private val athenaProperties = AthenaProperties(
    auditEventType = AuditEventType.PRISONER,
    s3BucketName = "hmpps-audit-bucket",
    databaseName = "the-database",
    tableName = "the-table",
    workGroupName = "the-workgroup",
    outputLocation = "the-location",
  )
}

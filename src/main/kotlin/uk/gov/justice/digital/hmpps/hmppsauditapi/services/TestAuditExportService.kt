package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.StaffAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.toAuditEvent
import java.nio.file.Files
import java.time.Clock
import java.util.UUID

@Service
class TestAuditExportService(
  private val staffAuditRepository: StaffAuditRepository,
  private val auditParquetWriter: AuditParquetWriter,
  private val clock: Clock,
) {
  @Transactional
  fun createBatch(): UUID {
    val batchId = UUID.randomUUID()
    val timestamp = clock.instant()
    staffAuditRepository.saveAll(
      (1..10).map {
        StaffAuditEvent(
          what = "INTEGRATION_TEST",
          `when` = timestamp,
          correlationId = batchId.toString(),
          who = "TEST_$batchId",
          service = TEST_SERVICE,
          details = """{"eventNumber":$it}""",
        )
      },
    )
    log.info("Created 10 synthetic STAFF audit events for batch {}", batchId)
    return batchId
  }

  fun download(batchId: UUID): ByteArray {
    val events = staffAuditRepository.findPage(
      pageable = PageRequest.of(0, 11),
      startDate = null,
      endDate = null,
      service = TEST_SERVICE,
      subjectId = null,
      subjectType = null,
      correlationId = batchId.toString(),
      what = "INTEGRATION_TEST",
      who = "TEST_$batchId",
    )
    if (events.isEmpty) {
      log.info("Synthetic audit batch {} not found", batchId)
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Test audit batch not found")
    }
    if (events.totalElements != 10L) {
      log.error("Synthetic audit batch {} contains {} events instead of 10", batchId, events.totalElements)
      throw ResponseStatusException(HttpStatus.CONFLICT, "Test audit batch must contain exactly 10 events")
    }
    val file = Files.createTempFile("test-audit-$batchId-", ".parquet")
    try {
      auditParquetWriter.write(events.content.asSequence().map { it.toAuditEvent() }, file)
      return Files.readAllBytes(file)
    } finally {
      Files.deleteIfExists(file)
    }
  }

  private companion object {
    const val TEST_SERVICE = "hmpps-audit-api-parquet-test"
    val log = LoggerFactory.getLogger(TestAuditExportService::class.java)
  }
}

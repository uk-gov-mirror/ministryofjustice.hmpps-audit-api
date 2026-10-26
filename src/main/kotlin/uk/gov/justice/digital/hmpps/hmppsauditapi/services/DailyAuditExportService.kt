package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityManager
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.toAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.DailyAuditExportResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.streams.asSequence

@Service
class DailyAuditExportService(
  private val staffAuditRepository: StaffAuditRepository,
  private val auditS3Client: AuditS3Client,
  private val athenaPropertiesFactory: AthenaPropertiesFactory,
  private val entityManager: EntityManager,
  private val telemetryClient: TelemetryClient,
  private val clock: Clock,
  transactionManager: PlatformTransactionManager,
) {
  private val readOnlyTransaction = TransactionTemplate(transactionManager).apply { isReadOnly = true }

  fun export(auditEventType: AuditEventType, date: LocalDate): DailyAuditExportResponse {
    validate(auditEventType, date)
    val startTime = clock.millis()
    val bucketName = athenaPropertiesFactory.getProperties(auditEventType).s3BucketName
    val file = Files.createTempFile("audit-${auditEventType.name.lowercase()}-$date-", ".parquet")
    try {
      val eventCount = readOnlyTransaction.execute { writeEventsForDate(date, file) }
      val key = if (eventCount > 0) auditS3Client.uploadDailyFile(file, bucketName, date) else null
      val fileSizeBytes = if (key != null) Files.size(file) else 0L
      val response = DailyAuditExportResponse(auditEventType, date, eventCount, key, fileSizeBytes)
      val durationMillis = clock.millis() - startTime
      log.info("Daily audit export complete: {} in {}ms", response, durationMillis)
      telemetryClient.trackEvent(
        "hmpps-audit-daily-export",
        mapOf(
          "auditEventType" to auditEventType.name,
          "date" to date.toString(),
          "eventCount" to eventCount.toString(),
          "fileSizeBytes" to fileSizeBytes.toString(),
          "durationMillis" to durationMillis.toString(),
        ),
      )
      return response
    } finally {
      Files.deleteIfExists(file)
    }
  }

  private fun validate(auditEventType: AuditEventType, date: LocalDate) {
    if (auditEventType != AuditEventType.STAFF) {
      throw ValidationException("Daily export is not supported for $auditEventType")
    }
    if (!date.isBefore(LocalDate.now(clock.withZone(ZoneOffset.UTC)))) {
      throw ValidationException("Date must be before today (UTC)")
    }
  }

  private fun writeEventsForDate(date: LocalDate, file: Path): Long {
    val start = date.atStartOfDay(ZoneOffset.UTC).toInstant()
    val end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
    return staffAuditRepository.streamByWhenFromInclusiveToExclusive(start, end).use { events ->
      auditS3Client.writeParquetFile(events.asSequence().map { detach(it).toAuditEvent() }, file)
    }
  }

  // Stops the persistence context growing with every row read from the stream
  private fun <T : Any> detach(entity: T): T = entity.also { entityManager.detach(it) }

  private companion object {
    private val log = LoggerFactory.getLogger(DailyAuditExportService::class.java)
  }
}

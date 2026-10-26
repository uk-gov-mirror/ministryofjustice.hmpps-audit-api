package uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import java.time.LocalDate

@Schema(description = "Result of exporting a day of audit events to a single Parquet file in S3")
data class DailyAuditExportResponse(
  val auditEventType: AuditEventType,
  val date: LocalDate,
  val eventCount: Long,
  @Schema(description = "S3 key of the uploaded file, null when there were no events for the day")
  val s3Key: String?,
  val fileSizeBytes: Long,
)

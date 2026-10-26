package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.DailyAuditExportResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.DailyAuditExportService
import java.time.LocalDate

@RestController
@RequestMapping("/audit", produces = [MediaType.APPLICATION_JSON_VALUE])
class DailyAuditExportController(
  private val dailyAuditExportService: DailyAuditExportService,
) {
  @PreAuthorize("hasRole('ROLE_AUDIT')")
  @PostMapping("/{auditEventType}/daily-export/{date}")
  fun exportDay(
    @PathVariable auditEventType: AuditEventType,
    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
  ): DailyAuditExportResponse = dailyAuditExportService.export(auditEventType, date)
}

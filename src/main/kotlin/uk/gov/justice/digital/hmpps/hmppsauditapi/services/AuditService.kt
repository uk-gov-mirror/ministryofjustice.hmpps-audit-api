package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent

@Service
class AuditService(private val telemetryClient: TelemetryClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun audit(auditEvent: AuditEvent) {
    log.info("About to audit $auditEvent")
    telemetryClient.trackEvent("hmpps-audit", auditEvent.asMap())
  }
}

private fun AuditEvent.asMap(): Map<String, String> {
  var items = mapOf("what" to this.what, "when" to this.`when`)
  this.who?.let { items = items + ("who" to it) }
  this.service?.let { items = items + ("service" to it) }
  this.details?.let { items = items + ("details" to it) }

  return items
}

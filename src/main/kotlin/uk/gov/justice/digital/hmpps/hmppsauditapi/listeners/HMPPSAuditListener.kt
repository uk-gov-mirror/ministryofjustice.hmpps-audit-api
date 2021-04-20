package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.time.Instant
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Service
class HMPPSAuditListener(
  private val auditService: AuditService,
  private val mapper: ObjectMapper
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @JmsListener(destination = "\${sqs.queue.name}")
  fun onAuditEvent(message: String) {
    log.debug("Received message $message")
    val auditEvent: AuditEvent = mapper.readValue(message, AuditEvent::class.java)
    auditService.audit(auditEvent)
  }

  @Entity
  @Table(name = "AuditEvent")
  data class AuditEvent(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    val what: String? = null,
    @Column(name = "occurred", nullable = false)
    val `when`: Instant? = null,
    val who: String? = null,
    val service: String? = null,
    val details: String? = null
  )
}

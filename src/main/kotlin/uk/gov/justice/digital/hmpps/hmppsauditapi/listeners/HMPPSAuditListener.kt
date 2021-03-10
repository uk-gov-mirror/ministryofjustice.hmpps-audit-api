package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService

@Service
class HMPPSAuditListener(
  private val auditService: AuditService,
  private val gson: Gson
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @JmsListener(destination = "\${sqs.queue.name}")
  fun onAuditEvent(message: String) {
    log.debug("Received message $message")
    val sqsMessage: SQSMessage = gson.fromJson(message, SQSMessage::class.java)
    val auditEvent: AuditEvent = gson.fromJson(sqsMessage.Message, AuditEvent::class.java)
    auditService.audit(auditEvent)
  }

  data class AuditEvent(
    val what: String,
    val `when`: String,
    val who: String?,
    val service: String?,
    val details: String?,
  )

  data class SQSMessage(val Message: String, val MessageId: String)
}

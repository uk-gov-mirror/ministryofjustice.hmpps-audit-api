package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mockingDetails
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.StaffAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toStaffAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest
import java.time.Instant
import java.util.concurrent.TimeUnit

@TestPropertySource(properties = ["hmpps.repository.saveToS3Bucket=false", "hmpps.applicationinsights.auditEventDetails=true"])
class AuditTestDatabaseAppInsightsOn @Autowired constructor(
  override var webTestClient: WebTestClient,
) : QueueListenerIntegrationTest() {

  private val basicAuditEvent = AuditEvent(what = "basicAuditEvent", service = "hmpps-audit-poc-ui")

  @Test
  fun `will consume an audit event message`() {
    val message = """
    {
      "what": "OFFENDER_DELETED",
      "when": "2021-01-25T12:30:00Z",
      "operationId": "badea6d876c62e2f5264c94c7b50875e",
      "subjectId": "y1dea6d876c62e2f5264c94c7b50875r",
      "subjectType": "PERSON",
      "who": "bobby.beans",
      "service": "offender-service",
      "details": "{ \"offenderId\": \"99\"}"
    }
    """

    await.atMost(5, TimeUnit.SECONDS) untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(awsSqsUrl).messageBody(message).build(),
    )

    await.atMost(5, TimeUnit.SECONDS) untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    verify(telemetryClient).trackEvent(
      eq("hmpps-audit"),
      check {
        assertThat(it["what"]).isEqualTo("OFFENDER_DELETED")
        assertThat(it["when"]).isEqualTo("2021-01-25T12:30:00Z")
        assertThat(it["operationId"]).isEqualTo("badea6d876c62e2f5264c94c7b50875e")
        assertThat(it["who"]).isEqualTo("bobby.beans")
        assertThat(it["service"]).isEqualTo("offender-service")
        assertThat(it["details"]).isEqualTo("{ \"offenderId\": \"99\"}")
      },
      isNull(),
    )
    verify(staffAuditRepository).save(any<StaffAuditEvent>())
  }

  @Test
  fun `save basic audit entry to database`() {
    webTestClient.post()
      .uri("/audit")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(basicAuditEvent))
      .exchange()
      .expectStatus().isAccepted

    await.atMost(5, TimeUnit.SECONDS) untilCallTo { mockingDetails(staffAuditRepository).invocations.size } matches { it == 1 }

    verify(telemetryClient).trackEvent(eq("hmpps-audit"), any(), isNull())
    verify(staffAuditRepository).save(any<StaffAuditEvent>())
    verifyNoInteractions(auditS3Client)
  }

  @Test
  fun `save full audit entry to database`() {
    val auditEvent = AuditEvent(
      what = "OFFENDER_DELETED",
      `when` = Instant.parse("2021-04-01T15:15:30Z"),
      operationId = "cadea6d876c62e2f5264c94c7b50875e",
      subjectId = "bobby.beans",
      subjectType = "offender-service",
      correlationId = "{\"offenderId\": \"97\"}",
    )

    webTestClient.post()
      .uri("/audit")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(auditEvent))
      .exchange()
      .expectStatus().isAccepted

    await.atMost(5, TimeUnit.SECONDS) untilCallTo { mockingDetails(staffAuditRepository).invocations.size } matches { it == 1 }

    verify(telemetryClient).trackEvent(eq("hmpps-audit"), any(), isNull())
    verify(staffAuditRepository).save(auditEvent.toStaffAuditEvent())
    verifyNoInteractions(auditS3Client)
  }
}

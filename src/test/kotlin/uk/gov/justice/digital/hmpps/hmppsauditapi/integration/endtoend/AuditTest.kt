package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend

import com.amazonaws.services.sqs.AmazonSQS
import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class AuditTest {
  @Qualifier("awsSqsClient")
  @Autowired
  internal lateinit var awsSqsClient: AmazonSQS

  @Value("\${sqs.queue.name}")
  lateinit var queueName: String

  @MockBean
  lateinit var telemetryClient: TelemetryClient

  @Test
  fun `will consume an audit event message`() {
    val message = """
    {
      "what": "OFFENDER_DELETED",
      "when": "2021-01-25T12:30:00",
      "who": "bobby.beans",
      "service": "offender-service",
      "details": "{ \"offenderId\": \"99\"}"
    }
  """

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueName.queueUrl(), message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    verify(telemetryClient).trackEvent(
      eq("hmpps-audit"),
      check {
        assertThat(it["what"]).isEqualTo("OFFENDER_DELETED")
        assertThat(it["when"]).isEqualTo("2021-01-25T12:30:00")
        assertThat(it["who"]).isEqualTo("bobby.beans")
        assertThat(it["service"]).isEqualTo("offender-service")
        assertThat(it["details"]).isEqualTo("{ \"offenderId\": \"99\"}")
      },
      isNull()
    )
  }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes = awsSqsClient.getQueueAttributes(queueName.queueUrl(), listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun String.queueUrl(): String = awsSqsClient.getQueueUrl(this).queueUrl
}

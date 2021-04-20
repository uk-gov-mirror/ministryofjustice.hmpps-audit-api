package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuditRepositoryTest {

  @Autowired
  lateinit var auditRepository: AuditRepository

  @BeforeEach
  fun setup() {
    auditRepository.deleteAll()
  }

  @Test
  internal fun canWriteToRepositoryWithBasicAttributes() {
    val now = Instant.now()
    auditRepository.save(
      AuditEvent(
        what = "An Event with basic attributes",
        `when` = now
      )
    )

    assertThat(auditRepository.count()).isEqualTo(1)
    val auditEvent = auditRepository.findAll().first()

    assertThat(auditEvent.id).isNotNull
    assertThat(auditEvent.what).isEqualTo("An Event with basic attributes")
    assertThat(auditEvent.`when`).isEqualTo(now)
    assertThat(auditEvent.who).isNull()
    assertThat(auditEvent.service).isNull()
    assertThat(auditEvent.details).isNull()
  }

  @Test
  internal fun canWriteToRepositoryWithAllAttributes() {
    val details = """
      {
        "offenderId": "99"}
      """
    val now = Instant.now()

    auditRepository.save(
      AuditEvent(
        what = "An Event with all attributes",
        `when` = now,
        who = "John Smith",
        service = "current-service",
        details = details
      )
    )
    assertThat(auditRepository.count()).isEqualTo(1)
    val auditEvent = auditRepository.findAll().first()
    assertThat(auditEvent.id).isNotNull
    assertThat(auditEvent.what).isEqualTo("An Event with all attributes")
    assertThat(auditEvent.`when`).isEqualTo(now)
    assertThat(auditEvent.who).isEqualTo("John Smith")
    assertThat(auditEvent.service).isEqualTo("current-service")
    assertThat(auditEvent.details).isEqualTo(details)
  }
}

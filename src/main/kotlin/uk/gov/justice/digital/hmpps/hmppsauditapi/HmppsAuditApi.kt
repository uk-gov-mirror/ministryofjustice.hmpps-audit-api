package uk.gov.justice.digital.hmpps.hmppsauditapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsAuditApi

fun main(args: Array<String>) {
  runApplication<HmppsAuditApi>(*args)
}

package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.PersonOnProbationAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.PrisonerAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.StaffAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.addIfNotNull
import java.time.Instant
import java.util.UUID

private const val DEFAULT_SUBJECT_TYPE = "NOT_APPLICABLE"

@Schema(description = "Audit Event Insert Record")
data class AuditEvent(
  @Id
  @GeneratedValue
  @Hidden
  var id: UUID? = null,
  @Schema(description = "Detailed description of the Event", example = "COURT_REGISTER_BUILDING_UPDATE", required = true)
  val what: String,
  @Column(name = "occurred")
  @Schema(description = "When the Event occurred", example = "2021-04-01T15:15:30Z")
  val `when`: Instant = Instant.now(),
  @Schema(description = "The App Insights operation Id for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
  val operationId: String? = null,
  @Schema(description = "The subject ID for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
  val subjectId: String? = null,
  @Schema(description = "The subject type for the Event", example = "PERSON")
  val subjectType: String = DEFAULT_SUBJECT_TYPE,
  @Schema(description = "The correlation ID for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
  val correlationId: String? = null,
  @Schema(description = "Who initiated the Event", example = "fred.smith@myemail.com")
  val who: String? = null,
  @Schema(description = "Which service the Event relates to", example = "court-register")
  val service: String? = null,
  @Schema(description = "Additional information", example = "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}")
  val details: String? = null,
)

fun AuditEvent.toMap(includeDetails: Boolean): Map<String, String> {
  val items = mutableMapOf("what" to what, "when" to `when`.toString())
  items.addIfNotNull("who", who)
  items.addIfNotNull("operationId", operationId)
  items.addIfNotNull("subjectId", subjectId)
  items.addIfNotNull("subjectType", subjectType)
  items.addIfNotNull("correlationId", correlationId)
  items.addIfNotNull("service", service)
  if (includeDetails) items.addIfNotNull("details", details)
  return items.toMap()
}

fun AuditEvent.toPrisonerAuditEvent(): PrisonerAuditEvent = PrisonerAuditEvent(
  id = id,
  what = what,
  `when` = `when`,
  operationId = operationId,
  subjectId = subjectId,
  subjectType = subjectType,
  correlationId = correlationId,
  who = who,
  service = service,
  details = details,
)
fun AuditEvent.toPersonOnProbationAuditEvent(): PersonOnProbationAuditEvent = PersonOnProbationAuditEvent(
  id = id,
  what = what,
  `when` = `when`,
  operationId = operationId,
  subjectId = subjectId,
  subjectType = subjectType,
  correlationId = correlationId,
  who = who,
  service = service,
  details = details,
)

fun AuditEvent.toStaffAuditEvent(): StaffAuditEvent = StaffAuditEvent(
  id = id,
  what = what,
  `when` = `when`,
  operationId = operationId,
  subjectId = subjectId,
  subjectType = subjectType,
  correlationId = correlationId,
  who = who,
  service = service,
  details = details,
)

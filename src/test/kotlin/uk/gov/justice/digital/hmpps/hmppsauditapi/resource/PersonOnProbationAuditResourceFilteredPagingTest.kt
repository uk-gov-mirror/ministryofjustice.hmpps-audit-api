package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PersonOnProbationAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.PersonOnProbationAuditEvent
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = ["spring.datasource.url=jdbc:h2:mem:probation-filtered-paging;MODE=PostgreSQL"])
class PersonOnProbationAuditResourceFilteredPagingTest : IntegrationTest() {

  @Autowired
  private lateinit var personOnProbationAuditRepository: PersonOnProbationAuditRepository

  val whatRequestBody = JSONObject().put("what", "OFFENDER_DELETED")
  val whoRequestBody = JSONObject().put("who", "bobby.beans")
  val whoAndWhatRequestBody = JSONObject().put("who", "bobby.beans").put("what", "OFFENDER_DELETED")
  val startDateRequestBody = JSONObject().put("startDateTime", Instant.parse("2021-01-01T15:15:30Z")).put("endDateTime", Instant.parse("2021-12-31T17:17:30Z"))
  val endDateRequestBody = JSONObject().put("endDateTime", Instant.parse("2021-04-12T17:17:30Z"))
  val subjectIdRequestBody = JSONObject().put("subjectId", "da8ea6d876c62e2f5264c94c7b50867r")
  val subjectTypeRequestBody = JSONObject().put("subjectType", "PERSON")
  val dateRangeRequestBody = JSONObject().put("startDateTime", Instant.parse("2021-01-01T15:15:30Z"))
    .put("endDateTime", Instant.parse("2021-04-12T17:17:30Z"))

  val listOfAudits = listOf(
    PersonOnProbationAuditEvent(
      what = "MINIMUM_FIELDS_EVENT",
      `when` = Instant.parse("2021-04-04T17:17:30Z"),
    ),
    PersonOnProbationAuditEvent(
      what = "COURT_REGISTER_BUILDING_UPDATE",
      `when` = Instant.parse("2021-04-03T10:15:30Z"),
      operationId = "badea6d876c62e2f5264c94c7b50875b",
      subjectId = "da8ea6d876c62e2f5264c94c7b50867r",
      subjectType = "PERSON",
      correlationId = "correlationId1",
      who = "bobby.beans",
      service = "court-register",
      details = "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}",
    ),
    PersonOnProbationAuditEvent(
      what = "OFFENDER_DELETED",
      `when` = Instant.parse("2021-04-01T15:15:30Z"),
      operationId = "cadea6d876c62e2f5264c94c7b50875c",
      subjectId = "da4ea6d876c62e2f5264c94c7b508c57",
      subjectType = "PERSON",
      correlationId = "correlationId2",
      who = "bobby.beans",
      service = "launchpad",
      details = "{\"offenderId\": \"97\"}",
    ),
    PersonOnProbationAuditEvent(
      what = "OFFENDER_DELETED",
      `when` = Instant.parse("2020-12-31T08:11:30Z"),
      operationId = "dadea6d876c62e2f5264c94c7b50875d",
      subjectId = "mu2ea6d876c62e2f5264c94c7b508d57",
      subjectType = "PERSON",
      correlationId = "correlationId3",
      who = "freddy.frog",
      service = "launchpad",
      details = "{\"offenderId\": \"98\"}",
    ),
  )

  @BeforeAll
  fun `insert test audit events`() {
    personOnProbationAuditRepository.deleteAll()
    listOfAudits.forEach {
      personOnProbationAuditRepository.save(it)
    }
  }

  @AfterAll
  fun `remove test audit events`() {
    personOnProbationAuditRepository.deleteAll()
  }

  @Test
  fun `filter audit events by what`() {
    webTestClient.post().uri("/audit/persononprobation/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_PERSON_ON_PROBATION_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(whatRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(2)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(2)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].what").isEqualTo("OFFENDER_DELETED")
      .jsonPath("$.content[0].operationId").isEqualTo("cadea6d876c62e2f5264c94c7b50875c")
      .jsonPath("$.content[1].what").isEqualTo("OFFENDER_DELETED")
      .jsonPath("$.content[1].operationId").isEqualTo("dadea6d876c62e2f5264c94c7b50875d")
  }

  @Test
  fun `filter audit events by who`() {
    webTestClient.post().uri("/audit/persononprobation/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_PERSON_ON_PROBATION_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(whoRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(2)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(2)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].who").isEqualTo("bobby.beans")
      .jsonPath("$.content[0].operationId").isEqualTo("badea6d876c62e2f5264c94c7b50875b")
      .jsonPath("$.content[1].who").isEqualTo("bobby.beans")
      .jsonPath("$.content[1].operationId").isEqualTo("cadea6d876c62e2f5264c94c7b50875c")
  }

  @Test
  fun `filter audit events by what and who`() {
    webTestClient.post().uri("/audit/persononprobation/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_PERSON_ON_PROBATION_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(whoAndWhatRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(1)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(1)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].what").isEqualTo("OFFENDER_DELETED")
      .jsonPath("$.content[0].who").isEqualTo("bobby.beans")
      .jsonPath("$.content[0].operationId").isEqualTo("cadea6d876c62e2f5264c94c7b50875c")
  }

  @Test
  fun `filter audit events by startDateTme`() {
    webTestClient.post().uri("/audit/persononprobation/paged?page=0&size=2")
      .headers(setAuthorisation(roles = listOf("ROLE_PERSON_ON_PROBATION_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(startDateRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(2)
      .jsonPath("$.size").isEqualTo(2)
      .jsonPath("$.totalElements").isEqualTo(3)
      .jsonPath("$.totalPages").isEqualTo(2)
      .jsonPath("$.last").isEqualTo(false)
      .jsonPath("$.content[0].operationId").doesNotExist()
  }

  @Test
  fun `filter audit events by endDateTime`() {
    webTestClient.post().uri("/audit/persononprobation/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_PERSON_ON_PROBATION_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(endDateRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(3)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(4)
      .jsonPath("$.totalPages").isEqualTo(2)
      .jsonPath("$.last").isEqualTo(false)
      .jsonPath("$.content[0].operationId").doesNotExist()
  }

  @Test
  fun `filter audit events by date range`() {
    webTestClient.post().uri("/audit/persononprobation/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_PERSON_ON_PROBATION_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(dateRangeRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(3)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(3)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].operationId").doesNotExist()
  }

  @Test
  fun `filter audit events by subjectId`() {
    webTestClient.post().uri("/audit/persononprobation/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_PERSON_ON_PROBATION_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(subjectIdRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(1)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(1)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
  }

  @Test
  fun `filter audit events by subjectType`() {
    webTestClient.post().uri("/audit/persononprobation/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_PERSON_ON_PROBATION_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(subjectTypeRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(3)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(3)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
  }
}

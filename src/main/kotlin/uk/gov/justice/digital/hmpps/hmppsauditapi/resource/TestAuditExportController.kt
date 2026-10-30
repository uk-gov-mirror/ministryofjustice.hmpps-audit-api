package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.TestAuditExportService
import java.util.UUID

@RestController
@RequestMapping("/internal/integration-test/STAFF/parquet-batches")
@PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
class TestAuditExportController(
  private val testAuditExportService: TestAuditExportService,
) {
  @PostMapping
  fun createBatch(): Map<String, UUID> = mapOf("batchId" to testAuditExportService.createBatch())

  @GetMapping("/{batchId}")
  fun download(@PathVariable batchId: UUID): ResponseEntity<ByteArray> = ResponseEntity.ok()
    .contentType(MediaType.APPLICATION_OCTET_STREAM)
    .cacheControl(CacheControl.noStore())
    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("audit-test-$batchId.parquet").build().toString())
    .body(testAuditExportService.download(batchId))
}

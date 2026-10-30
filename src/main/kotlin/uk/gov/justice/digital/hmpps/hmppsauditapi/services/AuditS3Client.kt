package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.ZoneId
import java.util.Base64

@Service
class AuditS3Client(
  private val s3Client: S3Client,
  private val auditParquetWriter: AuditParquetWriter,
) {

  fun save(auditEvent: AuditEvent, bucketName: String) {
    val fileName = generateFilename(auditEvent)
    val parquetBytes = convertToParquetBytes(auditEvent)
    val md5Digest = MessageDigest.getInstance("MD5").digest(parquetBytes)
    val md5Base64 = Base64.getEncoder().encodeToString(md5Digest)

    val putObjectRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(fileName)
      .contentMD5(md5Base64)
      .build()

    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(parquetBytes))
  }

  private fun generateFilename(auditEvent: AuditEvent): String {
    val whenDateTime = auditEvent.`when`.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "year=${whenDateTime.year}/month=${whenDateTime.monthValue}/day=${whenDateTime.dayOfMonth}/user=${auditEvent.who}/" +
      "${auditEvent.id}.parquet"
  }

  private fun convertToParquetBytes(auditEvent: AuditEvent): ByteArray {
    val tempFile = File(System.getProperty("java.io.tmpdir"), "${auditEvent.id}.parquet")
    try {
      auditParquetWriter.write(sequenceOf(auditEvent), tempFile.toPath())
      return Files.readAllBytes(tempFile.toPath())
    } finally {
      Files.deleteIfExists(tempFile.toPath())
    }
  }
}

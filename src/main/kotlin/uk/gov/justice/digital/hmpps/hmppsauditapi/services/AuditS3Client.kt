package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.io.LocalOutputFile
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64

@Service
class AuditS3Client(
  private val s3Client: S3Client,
  private val schema: Schema,
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

  fun writeParquetFile(auditEvents: Sequence<AuditEvent>, file: Path): Long {
    var count = 0L
    parquetWriter(file).use { writer ->
      auditEvents.forEach {
        writer.write(toRecord(it))
        count++
      }
    }
    return count
  }

  fun uploadDailyFile(file: Path, bucketName: String, date: LocalDate): String {
    val key = dailyKey(date)
    val putObjectRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .contentMD5(md5Base64(file))
      .build()

    s3Client.putObject(putObjectRequest, RequestBody.fromFile(file))
    return key
  }

  private fun generateFilename(auditEvent: AuditEvent): String {
    val whenDateTime = auditEvent.`when`.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "year=${whenDateTime.year}/month=${whenDateTime.monthValue}/day=${whenDateTime.dayOfMonth}/user=${auditEvent.who}/" +
      "${auditEvent.id}.parquet"
  }

  private fun convertToParquetBytes(auditEvent: AuditEvent): ByteArray {
    val tempFile = File(System.getProperty("java.io.tmpdir"), "${auditEvent.id}.parquet")
    try {
      val record = toRecord(auditEvent)
      parquetWriter(tempFile.toPath()).use { writer ->
        writer.write(record)
      }
      return Files.readAllBytes(tempFile.toPath())
    } finally {
      Files.deleteIfExists(tempFile.toPath())
    }
  }

  private fun parquetWriter(file: Path) = AvroParquetWriter.builder<GenericRecord>(LocalOutputFile(file))
    .withSchema(schema)
    .withCompressionCodec(CompressionCodecName.SNAPPY)
    .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
    .withConf(Configuration())
    .build()

  private fun toRecord(auditEvent: AuditEvent): GenericRecord = GenericData.Record(schema).apply {
    put("id", auditEvent.id?.toString() ?: throw IllegalArgumentException("ID cannot be null"))
    put("what", auditEvent.what)
    put("when", auditEvent.`when`.toString())
    put("operationId", auditEvent.operationId)
    put("subjectId", auditEvent.subjectId)
    put("subjectType", auditEvent.subjectType)
    put("correlationId", auditEvent.correlationId)
    put("who", auditEvent.who)
    put("service", auditEvent.service)
    put("details", auditEvent.details)
  }

  private fun md5Base64(file: Path): String {
    val digest = MessageDigest.getInstance("MD5")
    DigestInputStream(Files.newInputStream(file), digest).use { it.transferTo(OutputStream.nullOutputStream()) }
    return Base64.getEncoder().encodeToString(digest.digest())
  }

  companion object {
    fun dailyKey(date: LocalDate): String = "daily/year=${date.year}/month=${date.monthValue}/day=${date.dayOfMonth}/audit.parquet"
  }
}

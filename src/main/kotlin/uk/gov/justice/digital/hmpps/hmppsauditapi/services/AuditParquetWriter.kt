package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.io.LocalOutputFile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import java.nio.file.Path

@Component
class AuditParquetWriter(private val schema: Schema) {
  fun write(auditEvents: Sequence<AuditEvent>, file: Path): Long {
    var count = 0L
    AvroParquetWriter.builder<GenericRecord>(LocalOutputFile(file))
      .withSchema(schema)
      .withCompressionCodec(CompressionCodecName.SNAPPY)
      .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
      .withConf(Configuration())
      .build().use { writer ->
        auditEvents.forEach {
          writer.write(toRecord(it))
          count++
        }
      }
    return count
  }

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
}

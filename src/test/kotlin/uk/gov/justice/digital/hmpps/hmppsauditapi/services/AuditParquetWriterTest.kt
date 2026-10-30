package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.io.LocalInputFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import java.nio.file.Files
import java.util.UUID

class AuditParquetWriterTest {
  private val writer = AuditParquetWriter(Schema.Parser().parse(javaClass.getResourceAsStream("/audit_event.avsc")))

  @Test
  fun `writes every event to a single file and returns the count`() {
    val events = (1..3).map { AuditEvent(id = UUID.randomUUID(), what = "event $it") }
    val file = Files.createTempFile("audit-parquet-test-", ".parquet")
    try {
      val count = writer.write(events.asSequence(), file)

      assertThat(count).isEqualTo(3)
      AvroParquetReader.builder<GenericRecord>(LocalInputFile(file)).build().use { reader ->
        val records = generateSequence { reader.read() }.toList()
        assertThat(records.map { it.get("id").toString() }).containsExactlyElementsOf(events.map { it.id.toString() })
        assertThat(records.map { it.get("what").toString() }).containsExactly("event 1", "event 2", "event 3")
      }
    } finally {
      Files.deleteIfExists(file)
    }
  }
}

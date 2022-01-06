package no.nav.amt.arena.acl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.arena.acl.kafka.KafkaConfiguration
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipInputStream

fun main() {
	val producer = ArenaKafkaProducer()
	producer.send("data/tiltak.zip", "tiltak")
	producer.send("data/tiltakgjennomforing.zip", "gjennomforing")
	producer.send("data/tiltakdeltaker.zip", "deltaker")
}

data class KafkaMessage(
	val table: String,

	@JsonProperty("op_type")
	val operation: String,

	@JsonProperty("op_ts")
	val operationTimestamp: String = getOperationTimestamp(LocalDateTime.now()),

	@JsonProperty("current_ts")
	val currentTimestamp: String = LocalDateTime.now().toString(),

	val pos: String,

	val before: Any? = null,

	val after: Any? = null
) {

	companion object {
		private fun getOperationTimestamp(ldt: LocalDateTime): String {
			val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
			return ldt.format(opTsFormatter)
		}
	}
}

class ArenaKafkaProducer {
	var pos = 1

	private val logger = LoggerFactory.getLogger(javaClass)
	private val kafkaConfiguration = KafkaConfiguration()

	private val kafkaProducer = kafkaConfiguration.kafkaProducer(kafkaConfiguration.localKafkaProperties())

	fun send(path: String, topic: String) {
		val objectMapper = jacksonObjectMapper()
		val string = readZip(path)

		val data: List<KafkaMessage> = objectMapper.readValue(string)

		data.forEach {
			val entry = it.copy(pos = "${pos++}")
			kafkaProducer.sendSync(ProducerRecord(topic, objectMapper.writeValueAsString(entry)))
		}

		logger.info("Sent ${data.size} messages on topic $topic")
	}

	private fun readZip(path: String): String {
		val inStream = this::class.java.classLoader.getResource(path)
		val zipInputStream = ZipInputStream(ByteArrayInputStream(inStream.readBytes()))

		val entry = zipInputStream.nextEntry // Should only be one file in zip
		val size = entry.size.toInt()
		val buffer = ByteArray(size)

		val bs = ByteArray(1024)
		var len = 0
		var off = 0

		while (zipInputStream.read(bs).also { len = it } != -1) {
			System.arraycopy(bs, 0, buffer, off, len)
			off += len
		}

		return StringReader(String(buffer)).readText()
	}

	private fun getOperationType(type: String): String {
		return when (type) {
			"INSERT" -> "I"
			"UPDATE" -> "U"
			"DELETE" -> "D"
			else -> throw IllegalArgumentException("type $type is not supported")
		}
	}

	private fun getKafkaProperties(): Properties {
		return KafkaPropertiesBuilder.producerBuilder()
			.withBrokerUrl(("localhost:9092"))
			.withBaseProperties()
			.withProducerId("amt-arena-acl")
			.withSerializers(StringSerializer::class.java, StringSerializer::class.java)
			.build()
	}

}


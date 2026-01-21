package no.nav.amt.arena.acl

import no.nav.amt.arena.acl.utils.ClassPathResourceUtils.readResourceAsText
import no.nav.amt.arena.acl.utils.JsonUtils.objectMapper
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.readValue
import java.util.Properties

fun main() {
	val producer = ArenaKafkaProducer()
	producer.send("data/arena-tiltakgjennomforingendret-v1.json", "gjennomforing")
	producer.send("data/arena-tiltakdeltakerendret-v1.json", "deltaker")
}

class ArenaKafkaProducer {
	private val log = LoggerFactory.getLogger(javaClass)
	private val kafkaProducer = KafkaProducerClientImpl<String, String>(getKafkaProperties())

	fun send(
		jsonFilePath: String,
		topic: String,
	) {
		val jsonFileContent = readResourceAsText(jsonFilePath)

		val data: List<JsonNode> = objectMapper.readValue(jsonFileContent)

		data.forEach {
			println(it.toString())
			kafkaProducer.sendSync(ProducerRecord(topic, it.toString()))
		}

		log.info("Sent ${data.size} messages on topic $topic")
	}

	private fun getKafkaProperties(): Properties =
		KafkaPropertiesBuilder
			.producerBuilder()
			.withBrokerUrl(("localhost:9092"))
			.withBaseProperties()
			.withProducerId("amt-arena-acl")
			.withSerializers(StringSerializer::class.java, StringSerializer::class.java)
			.build()
}

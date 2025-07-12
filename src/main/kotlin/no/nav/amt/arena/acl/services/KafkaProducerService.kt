package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KafkaProducerService(
	private val kafkaProducer: KafkaProducerClient<String, String>,
) {
	@Value("\${app.env.amtTopic}")
	lateinit var topic: String

	fun sendTilAmtTiltak(
		messageKey: UUID,
		data: AmtKafkaMessageDto<*>,
	) {
		val record = ProducerRecord(topic, messageKey.toString(), toJsonString(data))
		kafkaProducer.sendSync(record)
	}
}

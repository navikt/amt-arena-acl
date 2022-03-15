package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
open class KafkaProducerService(
	private val kafkaProducer: KafkaProducerClient<String, String>
) {

	private val objectMapper = ObjectMapperFactory.get()

	@Value("\${app.env.amtTopic}")
	lateinit var topic: String

	fun sendTilAmtTiltak(messageKey: UUID, data: AmtKafkaMessageDto<*>) {
		val record = ProducerRecord(topic, messageKey.toString(), objectMapper.writeValueAsString(data))
		kafkaProducer.sendSync(record)
	}

}

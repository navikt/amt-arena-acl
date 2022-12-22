package no.nav.amt.arena.acl.integration.kafka

import no.nav.amt.arena.acl.kafka.KafkaTopicProperties
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Component

@Component
class KafkaMessageSender(
	private val kafkaTopicProperties: KafkaTopicProperties,
	private val kafkaProducer: KafkaProducerClientImpl<String, String>,
) {
	fun publiserArenaGjennomforing(arenaGjennomforingId: String, jsonMessage: String) {
		kafkaProducer.sendSync(ProducerRecord(kafkaTopicProperties.arenaTiltakGjennomforingTopic, arenaGjennomforingId, jsonMessage))
	}

}

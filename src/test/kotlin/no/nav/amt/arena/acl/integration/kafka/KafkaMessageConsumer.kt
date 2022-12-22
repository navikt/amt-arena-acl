package no.nav.amt.arena.acl.integration.kafka

import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.kafka.KafkaTopicProperties
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.KafkaConsumerClientConfig
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Component

@Component
class KafkaMessageConsumer(
	kafkaTopicProperties: KafkaTopicProperties,
	kafkaProperties: KafkaProperties,
) {

	private val records = Collections.synchronizedList(mutableListOf<ConsumerRecord<String, String>>())

	private val client: KafkaConsumerClient

	init {
		val configs = listOf(
			kafkaTopicProperties.arenaSakTopic,
			kafkaTopicProperties.arenaTiltakTopic,
			kafkaTopicProperties.arenaTiltakDeltakerTopic,
			kafkaTopicProperties.arenaTiltakGjennomforingTopic
		).map(::createTopicConfig)

		client = KafkaConsumerClientBuilder.builder()
			.withProperties(kafkaProperties.consumer())
			.withTopicConfigs(configs)
			.build()

		client.start()
	}

	fun getRecords(topic: String): List<ConsumerRecord<String, String>> {
		return records.filter { it.topic == topic }
	}

	private fun createTopicConfig(topic: String): KafkaConsumerClientBuilder.TopicConfig<String, String> {
		return KafkaConsumerClientBuilder.TopicConfig<String, String>()
			.withConsumerConfig(
				topic,
				Deserializers.stringDeserializer(),
				Deserializers.stringDeserializer(),
				::handleRecord
			)
	}

	private fun handleRecord(record: ConsumerRecord<String, String>) {
		records.add(record)
	}
}

data class KafkaMessage(
	val topic: String,
	val key: String,
	val value: String
)

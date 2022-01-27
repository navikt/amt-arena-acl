package no.nav.amt.arena.acl.goldengate

import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.kafka.KafkaTopicProperties
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import org.springframework.stereotype.Component

@Component
open class GoldenGateKafkaConsumer(
	kafkaTopicProperties: KafkaTopicProperties,
	kafkaProperties: KafkaProperties,
	private val arenaMessageProcessor: ArenaMessageProcessor,
) {
	private val client: KafkaConsumerClient

	init {
		val topicConfigs = listOf(
			kafkaTopicProperties.arenaTiltakTopic,
			kafkaTopicProperties.arenaTiltakGjennomforingTopic,
			kafkaTopicProperties.arenaTiltakDeltakerTopic
		).map { topic ->
			KafkaConsumerClientBuilder.TopicConfig<String, String>()
				.withLogging()
				.withConsumerConfig(
					topic,
					stringDeserializer(),
					stringDeserializer(),
					arenaMessageProcessor::handleArenaGoldenGateRecord
				)
		}

		client = KafkaConsumerClientBuilder.builder()
			.withProperties(kafkaProperties.consumer())
			.withTopicConfigs(topicConfigs)
			.build()

		client.start()
	}

}

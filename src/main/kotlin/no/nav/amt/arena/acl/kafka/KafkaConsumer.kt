package no.nav.amt.arena.acl.kafka

import no.nav.amt.arena.acl.services.ArenaMessageConsumerService
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
open class KafkaConsumer(
	kafkaTopicProperties: KafkaTopicProperties,
	kafkaProperties: KafkaProperties,
	private val arenaMessageConsumerService: ArenaMessageConsumerService,
) {

	private val client: KafkaConsumerClient

	private val log = LoggerFactory.getLogger(javaClass)

	init {
		val topics = listOf(
			kafkaTopicProperties.arenaTiltakGjennomforingTopic,
			kafkaTopicProperties.arenaTiltakDeltakerTopic,
			kafkaTopicProperties.arenaHistTiltakDeltakerTopic
		)

		val topicConfigs = topics.map { topic ->
			KafkaConsumerClientBuilder.TopicConfig<String, String>()
				.withLogging()
				.withConsumerConfig(
					topic,
					stringDeserializer(),
					stringDeserializer(),
					arenaMessageConsumerService::handleArenaGoldenGateRecord
				)
		}

		client = KafkaConsumerClientBuilder.builder()
			.withProperties(kafkaProperties.consumer())
			.withTopicConfigs(topicConfigs)
			.build()
	}

	@EventListener
	open fun onContextRefreshed(_event: ContextRefreshedEvent) = start()

	@EventListener
	open fun onContextClosed(_event: ContextClosedEvent) = stop()

	fun start() {
		log.info("Starting kafka consumer...")
		client.start()
	}

	fun stop() {
		log.info("Stopping kafka consumer...")
		client.stop()
	}
}

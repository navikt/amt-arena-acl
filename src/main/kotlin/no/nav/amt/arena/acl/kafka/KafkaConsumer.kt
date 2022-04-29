package no.nav.amt.arena.acl.kafka

import no.nav.amt.arena.acl.services.ArenaMessageProcessorService
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
open class KafkaConsumer(
	kafkaTopicProperties: KafkaTopicProperties,
	kafkaProperties: KafkaProperties,
	private val arenaMessageProcessorService: ArenaMessageProcessorService,
) {

	private val client: KafkaConsumerClient

	private val log = LoggerFactory.getLogger(javaClass)

	init {
		// Dette er midlertidig siden vi ikke har tilgang til sak-topicen i prod

		val erProd = System.getenv("NAIS_CLUSTER_NAME") == "prod-gcp"

		val topics = if (erProd) listOf(
			kafkaTopicProperties.arenaTiltakTopic,
			kafkaTopicProperties.arenaTiltakGjennomforingTopic,
			kafkaTopicProperties.arenaTiltakDeltakerTopic,
		) else listOf(
			kafkaTopicProperties.arenaTiltakTopic,
			kafkaTopicProperties.arenaTiltakGjennomforingTopic,
			kafkaTopicProperties.arenaTiltakDeltakerTopic,
			kafkaTopicProperties.arenaSakTopic
		)

		val topicConfigs = topics.map { topic ->
			KafkaConsumerClientBuilder.TopicConfig<String, String>()
				.withLogging()
				.withConsumerConfig(
					topic,
					stringDeserializer(),
					stringDeserializer(),
					arenaMessageProcessorService::handleArenaGoldenGateRecord
				)
		}

		client = KafkaConsumerClientBuilder.builder()
			.withProperties(kafkaProperties.consumer())
			.withTopicConfigs(topicConfigs)
			.build()
	}

	@EventListener
	open fun onApplicationEvent(_event: ContextRefreshedEvent?) {
		log.info("Starting kafka consumer...")
		client.start()
	}

}

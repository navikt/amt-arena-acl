package no.nav.amt.arena.acl.integration.kafka

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.kafka.amt.*
import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.LocalDateTime
import java.util.*

class KafkaAmtIntegrationConsumer(
	kafkaProperties: KafkaProperties,
	topic: String
) {

	private val client: KafkaConsumerClient


	companion object {
		private val deltakerSubsctiptions = mutableMapOf<UUID, (wrapper: AmtKafkaMessageDto<AmtDeltaker>) -> Unit>()
		private val gjennomforingSubscriptions = mutableMapOf<UUID, (wrapper: AmtKafkaMessageDto<AmtGjennomforing>) -> Unit>()

		fun subscribeDeltaker(handler: (record: AmtKafkaMessageDto<AmtDeltaker>) -> Unit): UUID {
			val id = UUID.randomUUID()
			deltakerSubsctiptions[id] = handler

			return id
		}

		fun subscribeGjennomforing(handler: (record: AmtKafkaMessageDto<AmtGjennomforing>) -> Unit): UUID {
			val id = UUID.randomUUID()
			gjennomforingSubscriptions[id] = handler

			return id
		}

		fun reset() {
			deltakerSubsctiptions.clear()
			gjennomforingSubscriptions.clear()
		}
	}


	init {
		val config = KafkaConsumerClientBuilder.TopicConfig<String, String>()
			.withLogging()
			.withConsumerConfig(
				topic,
				stringDeserializer(),
				stringDeserializer(),
				::handle
			)

		client = KafkaConsumerClientBuilder.builder()
			.withProperties(kafkaProperties.consumer())
			.withTopicConfig(config)
			.build()

		client.start()
	}

	private fun handle(record: ConsumerRecord<String, String>) {
		val unknownMessageWrapper = fromJson(record.value(), UnknownMessageWrapper::class.java)

		when (unknownMessageWrapper.type) {
			PayloadType.DELTAKER -> {
				val deltakerPayload =
					ObjectMapperFactory.get().treeToValue(unknownMessageWrapper.payload, AmtDeltaker::class.java)
				val message = toKnownMessageWrapper(deltakerPayload, unknownMessageWrapper)
				deltakerSubsctiptions.values.forEach { it.invoke(message) }

			}
			PayloadType.GJENNOMFORING -> {
				val gjennomforingPayload =
					ObjectMapperFactory.get().treeToValue(unknownMessageWrapper.payload, AmtGjennomforing::class.java)
				val message = toKnownMessageWrapper(gjennomforingPayload, unknownMessageWrapper)
				gjennomforingSubscriptions.values.forEach { it.invoke(message) }
			}
			else -> throw IllegalStateException("${unknownMessageWrapper.type} does not have a handler.")
		}
	}

	private fun <T> toKnownMessageWrapper(payload: T, unknownMessageWrapper: UnknownMessageWrapper): AmtKafkaMessageDto<T> {
		return AmtKafkaMessageDto(
			transactionId = UUID.fromString(unknownMessageWrapper.transactionId),
			type = unknownMessageWrapper.type,
			timestamp = unknownMessageWrapper.timestamp,
			operation = unknownMessageWrapper.operation,
			payload = payload
		)
	}

	fun <T> fromJson(jsonStr: String, clazz: Class<T>): T {
		return ObjectMapperFactory.get().readValue(jsonStr, clazz)
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	data class UnknownMessageWrapper(
		val transactionId: String,
		val type: PayloadType,
		val timestamp: LocalDateTime,
		val operation: AmtOperation,
		val payload: JsonNode
	)

}

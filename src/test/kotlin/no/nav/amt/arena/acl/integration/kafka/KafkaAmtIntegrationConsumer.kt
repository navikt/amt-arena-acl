package no.nav.amt.arena.acl.integration.kafka

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonNode
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID

class KafkaAmtIntegrationConsumer(
	kafkaProperties: KafkaProperties,
	topic: String,
) {
	private val client: KafkaConsumerClient

	companion object {
		private val deltakerSubsctiptions = mutableMapOf<UUID, (wrapper: AmtKafkaMessageDto<AmtDeltaker>) -> Unit>()
	}

	init {
		val config =
			KafkaConsumerClientBuilder
				.TopicConfig<String, String>()
				.withLogging()
				.withConsumerConfig(
					topic,
					stringDeserializer(),
					stringDeserializer(),
					::handle,
				)

		client =
			KafkaConsumerClientBuilder
				.builder()
				.withProperties(kafkaProperties.consumer())
				.withTopicConfig(config)
				.build()

		client.start()
	}

	private fun handle(record: ConsumerRecord<String, String>) {
		val unknownMessageWrapper = fromJsonString<UnknownMessageWrapper>(record.value())

		when (unknownMessageWrapper.type) {
			PayloadType.DELTAKER -> {
				val deltakerPayload =
					fromJsonNode<AmtDeltaker>(unknownMessageWrapper.payload)
				val message = toKnownMessageWrapper(deltakerPayload, unknownMessageWrapper)
				deltakerSubsctiptions.values.forEach { it.invoke(message) }
			}
		}
	}

	private fun <T> toKnownMessageWrapper(
		payload: T,
		unknownMessageWrapper: UnknownMessageWrapper,
	): AmtKafkaMessageDto<T> =
		AmtKafkaMessageDto(
			transactionId = UUID.fromString(unknownMessageWrapper.transactionId),
			type = unknownMessageWrapper.type,
			timestamp = unknownMessageWrapper.timestamp,
			operation = unknownMessageWrapper.operation,
			payload = payload,
		)

	@JsonIgnoreProperties(ignoreUnknown = true)
	data class UnknownMessageWrapper(
		val transactionId: String,
		val type: PayloadType,
		val timestamp: LocalDateTime,
		val operation: AmtOperation,
		val payload: JsonNode,
	)
}

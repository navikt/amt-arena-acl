package no.nav.amt.arena.acl.goldengate

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.kafka.KafkaTopicProperties
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
open class GoldenGateKafkaConsumer(
	kafkaTopicProperties: KafkaTopicProperties,
	kafkaProperties: KafkaProperties,
	private val arenaDataRepository: ArenaDataRepository
) {
	private val client: KafkaConsumerClient

	private val mapper = jacksonObjectMapper()

	init {
		val topicConfigs = listOf(
			kafkaTopicProperties.arenaTiltakTopic,
			kafkaTopicProperties.arenaTiltaksgjennomforingTopic,
			kafkaTopicProperties.arenaTiltakDeltakerTopic
		).map { topic ->
			KafkaConsumerClientBuilder.TopicConfig<String, String>()
				.withLogging()
				.withConsumerConfig(
					topic,
					stringDeserializer(),
					stringDeserializer(),
					Consumer<ConsumerRecord<String, String>> { handle(it.value()) }
				)
		}

		client = KafkaConsumerClientBuilder.builder()
			.withProperties(kafkaProperties.consumer())
			.withTopicConfigs(topicConfigs)
			.build()

		client.start()
	}

	private fun handle(value: String) {
		GlobalScope.launch {
			val data = mapper.readValue(value, ArenaWrapper::class.java).toArenaData()
			arenaDataRepository.upsert(data)
		}

	}

	private fun ArenaWrapper.toArenaData() = ArenaData(
		arenaTableName = this.table,
		arenaId = this.arenaId,
		operation = this.operation.toAmtOperation(),
		operationPosition = this.operationPosition,
		operationTimestamp = this.operationTimestamp,
		before = this.before,
		after = this.after
	)

	private fun ArenaOperation.toAmtOperation(): AmtOperation {
		return when (this) {
			ArenaOperation.I -> AmtOperation.CREATED
			ArenaOperation.U -> AmtOperation.MODIFIED
			ArenaOperation.D -> AmtOperation.DELETED
		}
	}
}

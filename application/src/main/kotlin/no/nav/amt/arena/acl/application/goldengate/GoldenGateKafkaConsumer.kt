package no.nav.amt.arena.acl.application.goldengate

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.amt.arena.acl.application.ArenaDataService
import no.nav.amt.arena.acl.application.kafka.KafkaProperties
import no.nav.amt.arena.acl.application.kafka.KafkaTopicProperties
import no.nav.amt.arena.acl.application.models.ArenaData
import no.nav.amt.arena.acl.application.models.OperationType
import no.nav.amt.arena.acl.application.models.arena.GoldenGateKafkaDto
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

@Component
open class GoldenGateKafkaConsumer(
    kafkaTopicProperties: KafkaTopicProperties,
    kafkaProperties: KafkaProperties,
    private val arenaDataService: ArenaDataService
) {
    private val client: KafkaConsumerClient

    private val mapper = jacksonObjectMapper()
    private val operationTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

    init {
        val topicConfigs = listOf(
            kafkaTopicProperties.arenaTiltakTopic,
            kafkaTopicProperties.arenaTiltaksgruppeTopic,
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
        arenaDataService.store(mapper.readValue(value, GoldenGateKafkaDto::class.java).toArenaData())
    }

    private fun GoldenGateKafkaDto.toArenaData(): ArenaData {
        val beforeString = mapper.writeValueAsString(before)
        val afterString = mapper.writeValueAsString(after)

        return ArenaData(
            tableName = table,
            operationType = OperationType.fromArena(operationType),
            operationPosition = pos.toLong(),
            operationTimestamp = LocalDateTime.parse(operationTimestamp, operationTimestampFormatter),
            before = if (beforeString != null && beforeString != "null") beforeString else null,
            after = if (afterString != null && afterString != "null") afterString else null,
        )
    }
}

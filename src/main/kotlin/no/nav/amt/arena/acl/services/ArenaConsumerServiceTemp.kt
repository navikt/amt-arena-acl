package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.consumer.ArenaDeltakerConsumerTemp
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.DateUtils.parseArenaDateTime
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonNode
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.amt.arena.acl.utils.removeNullCharacters
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
open class ArenaConsumerServiceTemp(
	private val arenaDeltakerConsumerTemp: ArenaDeltakerConsumerTemp,
) {
	fun handleArenaGoldenGateRecord(record: ConsumerRecord<String, String>) {
		val recordValue = record.value().removeNullCharacters()
		val messageDto = fromJsonString<ArenaKafkaMessageDto>(recordValue)

		processArenaKafkaMessage(messageDto)
	}

	private fun processArenaKafkaMessage(messageDto: ArenaKafkaMessageDto) {
		if(messageDto.table == ARENA_DELTAKER_TABLE_NAME) {
			arenaDeltakerConsumerTemp.handleArenaMessage(toArenaKafkaMessage(messageDto))
		}
	}

	private inline fun <reified D> toArenaKafkaMessage(messageDto: ArenaKafkaMessageDto): ArenaKafkaMessage<D> {
		return ArenaKafkaMessage(
			arenaTableName = messageDto.table,
			operationType = AmtOperation.fromArenaOperationString(messageDto.opType),
			operationTimestamp = parseArenaDateTime(messageDto.opTs),
			operationPosition = messageDto.pos,
			before = messageDto.before?.let { fromJsonNode(it) },
			after =  messageDto.after?.let { fromJsonNode(it) }
		)
	}
}

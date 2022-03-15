package no.nav.amt.arena.acl.domain.db

import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import java.time.LocalDateTime

data class ArenaDataUpsertInput(
	val arenaTableName: String,
	val arenaId: String,
	val operation: AmtOperation,
	val operationPosition: String,
	val operationTimestamp: LocalDateTime,
	val ingestStatus: IngestStatus = IngestStatus.NEW,
	val ingestedTimestamp: LocalDateTime? = null,
	val before: String? = null,
	val after: String? = null,
	val note: String? = null
)

private val objectMapper = ObjectMapperFactory.get()

fun ArenaKafkaMessage<*>.toUpsertInput(arenaId: String, ingestStatus: IngestStatus, note: String? = null): ArenaDataUpsertInput {
	return ArenaDataUpsertInput(
		arenaTableName = this.arenaTableName,
		arenaId = arenaId,
		operation = this.operationType,
		operationPosition = this.operationPosition,
		operationTimestamp = this.operationTimestamp,
		ingestStatus = ingestStatus,
		ingestedTimestamp = LocalDateTime.now(),
		before = this.before?.let { objectMapper.writeValueAsString(it) },
		after = this.after?.let { objectMapper.writeValueAsString(it) },
		note = note
	)
}

fun ArenaKafkaMessage<*>.toUpsertInputWithStatusHandled(arenaId: String): ArenaDataUpsertInput {
	return this.toUpsertInput(arenaId, IngestStatus.HANDLED, null)
}

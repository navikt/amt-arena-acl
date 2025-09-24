package no.nav.amt.arena.acl.domain.db

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import java.time.LocalDateTime

fun ArenaKafkaMessage<*>.toUpsertInput(
	arenaId: String,
	ingestStatus: IngestStatus,
	note: String? = null
) = ArenaDataUpsertInput(
	arenaTableName = this.arenaTableName,
	arenaId = arenaId,
	operation = this.operationType,
	operationPosition = this.operationPosition,
	operationTimestamp = this.operationTimestamp,
	ingestStatus = ingestStatus,
	ingestedTimestamp = LocalDateTime.now(),
	before = this.before?.let { toJsonString(it) },
	after = this.after?.let { toJsonString(it) },
	note = note
)

fun ArenaKafkaMessage<*>.toUpsertInputWithStatusHandled(
	arenaId: String,
	note: String? = null
): ArenaDataUpsertInput = this.toUpsertInput(arenaId, IngestStatus.HANDLED, note)

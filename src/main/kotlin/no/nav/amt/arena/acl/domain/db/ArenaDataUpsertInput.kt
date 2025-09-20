package no.nav.amt.arena.acl.domain.db

import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
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

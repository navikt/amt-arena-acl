package no.nav.amt.arena.acl.domain

import no.nav.amt.arena.acl.domain.amt.AmtOperation
import java.time.LocalDateTime

enum class IngestStatus {
	NEW,
	INGESTED,
	RETRY,
	FAILED,
	IGNORED
}

data class ArenaData(
	val id: Int = -1,
	val arenaTableName: String,
	val arenaId: String,
	val operation: AmtOperation,
	val operationPosition: String,
	val operationTimestamp: LocalDateTime,
	val ingestStatus: IngestStatus = IngestStatus.NEW,
	val ingestedTimestamp: LocalDateTime? = null,
	val ingestAttempts: Int = 0,
	val lastAttempted: LocalDateTime? = null,
	val before: String? = null,
	val after: String? = null
) {

	fun markAsIgnored() = this.copy(ingestStatus = IngestStatus.IGNORED)

	fun markAsIngested() = this.copy(
		ingestStatus = IngestStatus.INGESTED,
		ingestedTimestamp = LocalDateTime.now()
	)

	fun markAsFailed() = this.copy(ingestStatus = IngestStatus.FAILED)

	fun retry() = this.copy(
		ingestStatus = IngestStatus.RETRY,
		ingestAttempts = ingestAttempts + 1,
		lastAttempted = LocalDateTime.now()
	)
}

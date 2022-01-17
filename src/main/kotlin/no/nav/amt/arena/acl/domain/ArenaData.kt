package no.nav.amt.arena.acl.domain

import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import java.time.LocalDateTime

enum class IngestStatus {
	NEW,
	HANDLED,
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
	val before: JsonNode? = null,
	val after: JsonNode? = null,
	val note: String? = null
) {

	fun markAsIgnored(reason: String? = null) = this.copy(ingestStatus = IngestStatus.IGNORED, note = reason)

	fun markAsHandled() = this.copy(
		ingestStatus = IngestStatus.HANDLED,
		ingestedTimestamp = LocalDateTime.now(),
		note = null
	)

	fun markAsFailed(reason: String? = null) = this.copy(
		ingestStatus = IngestStatus.FAILED,
		ingestAttempts = ingestAttempts + 1,
		lastAttempted = LocalDateTime.now(),
		note = reason
	)

	fun retry(reason: String? = null) = this.copy(
		ingestStatus = IngestStatus.RETRY,
		ingestAttempts = ingestAttempts + 1,
		lastAttempted = LocalDateTime.now(),
		note = reason
	)
}

package no.nav.amt.arena.acl.domain.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import java.time.LocalDateTime

enum class IngestStatus {
	NEW,
	HANDLED,
	RETRY,
	FAILED,
	IGNORED,
	INVALID
}

data class ArenaDataDbo(
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
	val before: JsonNode? = null, // TODO: should be String
	val after: JsonNode? = null, // TODO: should be String
	val note: String? = null
) {

	val objectMapper = ObjectMapperFactory.get()

	fun markAsIgnored(reason: String? = null) = this.copy(ingestStatus = IngestStatus.IGNORED, note = reason)

	fun markAsHandled() = this.copy(
		ingestStatus = IngestStatus.HANDLED,
		ingestedTimestamp = LocalDateTime.now(),
		note = null
	)

	fun markAsInvalid(reason: String) = this.copy(
		ingestStatus = IngestStatus.INVALID,
		note = reason
	)

	fun markAsFailed(reason: String? = null) = this.copy(
		ingestStatus = IngestStatus.FAILED,
		note = reason
	)

	fun retry(reason: String? = null) = this.copy(
		ingestStatus = IngestStatus.RETRY,
		ingestAttempts = ingestAttempts + 1,
		lastAttempted = LocalDateTime.now(),
		note = reason
	)

	inline fun <reified T> jsonObject(node: JsonNode?): T? {
		if (node == null) {
			return null
		}

		return objectMapper.treeToValue(node, T::class.java)
	}


}

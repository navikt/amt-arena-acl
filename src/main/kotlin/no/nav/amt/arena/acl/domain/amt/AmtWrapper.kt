package no.nav.amt.arena.acl.domain.amt

import java.time.LocalDateTime
import java.util.*

enum class AmtOperation {
	CREATED,
	MODIFIED,
	DELETED
}

data class AmtWrapper<T : AmtPayload>(
	val transactionId: UUID = UUID.randomUUID(),
	val source: String = "AMT_ARENA_ACL",
	val type: String,
	val timestamp: LocalDateTime = LocalDateTime.now(),
	val operation: AmtOperation,
	val before: T,
	val after: T
)

interface AmtPayload

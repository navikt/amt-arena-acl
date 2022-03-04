package no.nav.amt.arena.acl.domain.kafka.amt

import java.time.LocalDateTime
import java.util.*

enum class AmtOperation {
	CREATED,
	MODIFIED,
	DELETED
}

enum class PayloadType {
	DELTAKER,
	GJENNOMFORING
}

data class AmtWrapper<T>(
	val transactionId: UUID = UUID.randomUUID(),
	val source: String = "AMT_ARENA_ACL",
	val type: PayloadType,
	val timestamp: LocalDateTime = LocalDateTime.now(),
	val operation: AmtOperation,
	val payload: T?
)

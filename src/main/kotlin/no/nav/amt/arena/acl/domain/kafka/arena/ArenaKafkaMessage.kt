package no.nav.amt.arena.acl.domain.kafka.arena

import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.exceptions.ValidationException
import java.time.LocalDateTime

data class ArenaKafkaMessage<D>(
	val arenaTableName: String,
	val operationType: AmtOperation,
	val operationTimestamp: LocalDateTime,
	val operationPosition: String,
	val before: D?,
	val after: D?
) {
	fun getData(): D {
		return when (operationType) {
			AmtOperation.CREATED -> after ?: throw ValidationException("Message with opType=CREATED is missing 'after'")
			AmtOperation.MODIFIED -> after ?: throw ValidationException("Message with opType=MODIFIED is missing 'after'")
			AmtOperation.DELETED -> before ?: throw ValidationException("Message with opType=DELETED is missing 'before'")
		}
	}
}


typealias ArenaGjennomforingKafkaMessage = ArenaKafkaMessage<ArenaGjennomforing>

typealias ArenaDeltakerKafkaMessage = ArenaKafkaMessage<ArenaDeltaker>

typealias ArenaHistDeltakerKafkaMessage = ArenaKafkaMessage<ArenaHistDeltaker>

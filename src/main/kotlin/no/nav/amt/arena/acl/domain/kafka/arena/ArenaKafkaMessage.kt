package no.nav.amt.arena.acl.domain.kafka.arena

import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
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
			AmtOperation.CREATED -> after ?: throw NoSuchElementException("Message with opType=CREATED is missing 'after'")
			AmtOperation.MODIFIED -> after ?: throw NoSuchElementException("Message with opType=MODIFIED is missing 'after'")
			AmtOperation.DELETED -> before ?: throw NoSuchElementException("Message with opType=DELETED is missing 'before'")
		}
	}
}

typealias ArenaTiltakKafkaMessage = ArenaKafkaMessage<ArenaTiltak>

typealias ArenaGjennomforingKafkaMessage = ArenaKafkaMessage<ArenaGjennomforing>

typealias ArenaDeltakerKafkaMessage = ArenaKafkaMessage<ArenaDeltaker>

typealias ArenaSakKafkaMessage = ArenaKafkaMessage<ArenaSak>

package no.nav.amt.arena.acl.integration.commands.tiltak

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaOperation
import no.nav.amt.arena.acl.utils.ARENA_TILTAK_TABLE_NAME
import java.time.LocalDateTime

class SlettTiltakCommand(
	val kode: String = "INDOPPFAG",
	val navn: String
) : TiltakCommand() {

	override fun execute(
		position: String,
		executor: (wrapper: ArenaKafkaMessageDto, kode: String) -> TiltakResult
	): TiltakResult {
		val wrapper = ArenaKafkaMessageDto(
			table = ARENA_TILTAK_TABLE_NAME,
			opType = ArenaOperation.D.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = position,
			before = createPayload(kode, navn),
			after = null
		)

		return executor.invoke(wrapper, kode)

	}
}

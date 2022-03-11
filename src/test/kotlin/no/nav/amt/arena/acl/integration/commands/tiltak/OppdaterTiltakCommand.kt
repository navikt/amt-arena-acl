package no.nav.amt.arena.acl.integration.commands.tiltak

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaOperation
import no.nav.amt.arena.acl.utils.ARENA_TILTAK_TABLE_NAME
import java.time.LocalDateTime

class OppdaterTiltakCommand(
	val kode: String = "INDOPPFAG",
	val gammeltNavn: String,
	val nyttNavn: String
) : TiltakCommand() {

	override fun execute(position: String, executor: (wrapper: ArenaKafkaMessageDto, kode: String) -> TiltakResult): TiltakResult {
		val wrapper = ArenaKafkaMessageDto(
			table = ARENA_TILTAK_TABLE_NAME,
			opType = ArenaOperation.U.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = position,
			before = createPayload(kode, gammeltNavn),
			after = createPayload(kode, nyttNavn)
		)

		return executor.invoke(wrapper, kode)
	}
}

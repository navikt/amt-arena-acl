package no.nav.amt.arena.acl.integration.commands.tiltak

import no.nav.amt.arena.acl.domain.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.utils.TILTAK_TABLE_NAME
import java.time.LocalDateTime

class OppdaterTiltakCommand(
	val kode: String = "INDOPPFAG",
	val gammeltNavn: String,
	val nyttNavn: String
) : TiltakCommand() {

	override fun execute(position: String, executor: (wrapper: ArenaWrapper, kode: String) -> TiltakResult): TiltakResult {
		val wrapper = ArenaWrapper(
			table = TILTAK_TABLE_NAME,
			operation = ArenaOperation.U,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = position,
			before = createPayload(kode, gammeltNavn),
			after = createPayload(kode, nyttNavn)
		)

		return executor.invoke(wrapper, kode)
	}
}
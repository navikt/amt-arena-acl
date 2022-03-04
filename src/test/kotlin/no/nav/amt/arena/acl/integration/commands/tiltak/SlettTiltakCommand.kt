package no.nav.amt.arena.acl.integration.commands.tiltak

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaWrapper
import no.nav.amt.arena.acl.utils.ARENA_TILTAK_TABLE_NAME
import java.time.LocalDateTime

class SlettTiltakCommand(
	val kode: String = "INDOPPFAG",
	val navn: String
) : TiltakCommand() {

	override fun execute(
		position: String,
		executor: (wrapper: ArenaWrapper, kode: String) -> TiltakResult
	): TiltakResult {
		val wrapper = ArenaWrapper(
			table = ARENA_TILTAK_TABLE_NAME,
			operation = ArenaOperation.D,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = position,
			before = createPayload(kode, navn),
			after = null
		)

		return executor.invoke(wrapper, kode)

	}
}

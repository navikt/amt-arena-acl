package no.nav.amt.arena.acl.integration.commands.gjennomforing

import no.nav.amt.arena.acl.domain.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.utils.TILTAKGJENNOMFORING_TABLE_NAME
import java.time.LocalDateTime

class NyGjennomforingCommand(
	private val input: GjennomforingInput
) : GjennomforingCommand() {

	override fun execute(
		position: String,
		executor: (wrapper: ArenaWrapper) -> GjennomforingResult
	): GjennomforingResult {
		val wrapper = ArenaWrapper(
			table = TILTAKGJENNOMFORING_TABLE_NAME,
			operation = ArenaOperation.I,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = position,
			before = null,
			after = createPayload(input)
		)

		return executor.invoke(wrapper)
	}

}

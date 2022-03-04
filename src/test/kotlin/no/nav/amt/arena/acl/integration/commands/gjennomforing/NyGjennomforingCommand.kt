package no.nav.amt.arena.acl.integration.commands.gjennomforing

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaWrapper
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import java.time.LocalDateTime

class NyGjennomforingCommand(
	private val input: GjennomforingInput
) : GjennomforingCommand() {

	override fun execute(
		position: String,
		executor: (wrapper: ArenaWrapper) -> GjennomforingResult
	): GjennomforingResult {
		val wrapper = ArenaWrapper(
			table = ARENA_GJENNOMFORING_TABLE_NAME,
			operation = ArenaOperation.I,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = position,
			before = null,
			after = createPayload(input)
		)

		return executor.invoke(wrapper)
	}

}

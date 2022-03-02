package no.nav.amt.arena.acl.integration.commands.deltaker

import no.nav.amt.arena.acl.domain.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import java.time.LocalDateTime

class NyDeltakerCommand(private val input: DeltakerInput) : DeltakerCommand() {

	override fun execute(position: String, executor: (wrapper: ArenaWrapper) -> DeltakerResult): DeltakerResult {
		val wrapper = ArenaWrapper(
			table = ARENA_DELTAKER_TABLE_NAME,
			operation = ArenaOperation.I,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = position,
			before = null,
			after = createPayload(input)
		)

		return executor.invoke(wrapper)
	}
}

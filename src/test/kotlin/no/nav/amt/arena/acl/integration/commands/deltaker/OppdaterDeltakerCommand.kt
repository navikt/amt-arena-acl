package no.nav.amt.arena.acl.integration.commands.deltaker

import no.nav.amt.arena.acl.domain.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.utils.TILTAK_DELTAKER_TABLE_NAME
import java.time.LocalDateTime

class OppdaterDeltakerCommand(
	val oldDeltakerData: DeltakerInput,
	val updatedDeltakerData: DeltakerInput
) : DeltakerCommand() {

	override fun execute(position: String, executor: (wrapper: ArenaWrapper) -> DeltakerResult): DeltakerResult {
		val wrapper = ArenaWrapper(
			table = TILTAK_DELTAKER_TABLE_NAME,
			operation = ArenaOperation.U,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = position,
			before = createPayload(oldDeltakerData),
			after = createPayload(updatedDeltakerData)
		)

		return executor.invoke(wrapper)
	}

}

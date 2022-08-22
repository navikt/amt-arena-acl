package no.nav.amt.arena.acl.integration.commands.deltaker

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaOperation
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import java.time.LocalDateTime

class DeleteDeltakerCommand(private val input: DeltakerInput) : DeltakerCommand() {

	override fun execute(
		position: String,
		executor: (wrapper: ArenaKafkaMessageDto) -> DeltakerResult
	): DeltakerResult {
		val wrapper = ArenaKafkaMessageDto(
			table = ARENA_DELTAKER_TABLE_NAME,
			opType = ArenaOperation.D.name,
			opTs = LocalDateTime.now().format(opTsFormatter),
			pos = position,
			before = createPayload(input),
			after = null
		)

		return executor.invoke(wrapper)
	}
}

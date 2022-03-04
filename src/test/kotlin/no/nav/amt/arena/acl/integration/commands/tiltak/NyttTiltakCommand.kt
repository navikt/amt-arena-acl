package no.nav.amt.arena.acl.integration.commands.tiltak

<<<<<<< Updated upstream
import no.nav.amt.arena.acl.domain.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
=======
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaWrapper
>>>>>>> Stashed changes
import no.nav.amt.arena.acl.utils.ARENA_TILTAK_TABLE_NAME
import java.time.LocalDateTime
import java.util.*

class NyttTiltakCommand(
	val kode: String = "INDOPPFAG",
	val navn: String = UUID.randomUUID().toString()
) : TiltakCommand() {

	override fun execute(position: String, executor: (wrapper: ArenaWrapper, kode: String) -> TiltakResult): TiltakResult {
		val wrapper = ArenaWrapper(
			table = ARENA_TILTAK_TABLE_NAME,
			operation = ArenaOperation.I,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = position,
			before = null,
			after = createPayload(kode, navn)
		)

		return executor.invoke(wrapper, kode)
	}

}

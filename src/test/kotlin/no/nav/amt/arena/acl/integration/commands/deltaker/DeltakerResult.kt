package no.nav.amt.arena.acl.integration.commands.deltaker

import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import org.junit.jupiter.api.fail

data class DeltakerResult(
	val position: String,
	val arenaDataDbo: ArenaDataDbo,
	val translation: ArenaDataIdTranslationDbo?,
	val output: AmtKafkaMessageDto<AmtDeltaker>?
) {
	fun arenaData(check: (data: ArenaDataDbo) -> Unit): DeltakerResult {
		check.invoke(arenaDataDbo)
		return this
	}

	fun translation(check: (data: ArenaDataIdTranslationDbo) -> Unit): DeltakerResult {
		if (translation == null) {
			fail("Trying to get translation, but it is null")
		}

		check.invoke(translation)
		return this
	}

	fun output(check: (data: AmtKafkaMessageDto<AmtDeltaker>) -> Unit): DeltakerResult {
		if (output == null) {
			fail("Trying to get output, but it is null")
		}

		check.invoke(output)
		return this
	}

	fun result(check: (arenaDataDbo: ArenaDataDbo, translation: ArenaDataIdTranslationDbo?, output: AmtKafkaMessageDto<AmtDeltaker>?) -> Unit): DeltakerResult {
		check.invoke(arenaDataDbo, translation, output)
		return this
	}

	fun outgoingPayload(check: (payload: AmtDeltaker) -> Unit): DeltakerResult {
		if (output?.payload == null) {
			fail("Forsøker å hente payload på en outgoing melding som er null")
		}

		check.invoke(output.payload!!)
		return this
	}
}

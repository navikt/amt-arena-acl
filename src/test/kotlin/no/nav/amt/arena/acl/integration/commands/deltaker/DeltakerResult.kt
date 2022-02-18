package no.nav.amt.arena.acl.integration.commands.deltaker

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import org.junit.jupiter.api.fail

data class DeltakerResult(
	val position: String,
	val arenaData: ArenaData,
	val translation: ArenaDataIdTranslation?,
	val output: AmtWrapper<AmtDeltaker>?
) {
	fun arenaData(check: (data: ArenaData) -> Unit): DeltakerResult {
		check.invoke(arenaData)
		return this
	}

	fun translation(check: (data: ArenaDataIdTranslation) -> Unit): DeltakerResult {
		if (translation == null) {
			fail("Trying to get translation, but it is null")
		}

		check.invoke(translation)
		return this
	}

	fun output(check: (data: AmtWrapper<AmtDeltaker>) -> Unit): DeltakerResult {
		if (output == null) {
			fail("Trying to get output, but it is null")
		}

		check.invoke(output)
		return this
	}

	fun result(check: (arenaData: ArenaData, translation: ArenaDataIdTranslation?, output: AmtWrapper<AmtDeltaker>?) -> Unit): DeltakerResult {
		check.invoke(arenaData, translation, output)
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

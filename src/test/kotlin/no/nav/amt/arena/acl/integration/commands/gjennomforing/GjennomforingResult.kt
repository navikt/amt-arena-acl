package no.nav.amt.arena.acl.integration.commands.gjennomforing

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import org.junit.jupiter.api.fail

data class GjennomforingResult(
	val position: String,
	val arenaData: ArenaData,
	val translation: ArenaDataIdTranslation?,
	val output: AmtWrapper<AmtGjennomforing>?
) {

	fun arenaData(check: (data: ArenaData) -> Unit): GjennomforingResult {
		check.invoke(arenaData)
		return this
	}

	fun translation(check: (data: ArenaDataIdTranslation) -> Unit): GjennomforingResult {
		if (translation == null) {
			fail("Trying to get translation, but it is null")
		}

		check.invoke(translation)
		return this
	}

	fun output(check: (data: AmtWrapper<AmtGjennomforing>) -> Unit): GjennomforingResult {
		if (output == null) {
			fail("Trying to get output, but it is null")
		}

		check.invoke(output)
		return this
	}

	fun result(check: (arenaData: ArenaData, translation: ArenaDataIdTranslation?, output: AmtWrapper<AmtGjennomforing>?) -> Unit): GjennomforingResult {
		check.invoke(arenaData, translation, output)
		return this
	}

}

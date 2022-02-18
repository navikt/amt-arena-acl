package no.nav.amt.arena.acl.integration.commands.tiltak

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtTiltak

data class TiltakResult(
	val arenaData: ArenaData,
	val tiltak: AmtTiltak
) {

	fun arenaData(check: (data: ArenaData) -> Unit): TiltakResult {
		check.invoke(arenaData)
		return this
	}

	fun tiltak(check: (data: AmtTiltak) -> Unit): TiltakResult {
		check.invoke(tiltak)
		return this
	}
}

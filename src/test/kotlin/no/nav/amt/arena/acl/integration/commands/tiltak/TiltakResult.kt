package no.nav.amt.arena.acl.integration.commands.tiltak

import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.kafka.amt.AmtTiltak

data class TiltakResult(
	val arenaDataDbo: ArenaDataDbo,
	val tiltak: AmtTiltak
) {

	fun arenaData(check: (data: ArenaDataDbo) -> Unit): TiltakResult {
		check.invoke(arenaDataDbo)
		return this
	}

	fun tiltak(check: (data: AmtTiltak) -> Unit): TiltakResult {
		check.invoke(tiltak)
		return this
	}
}

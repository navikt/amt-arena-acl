package no.nav.amt.arena.acl.processors.converters

import org.springframework.dao.DataIntegrityViolationException

object GjennomforingStatusConverter {

	private val avsluttendeStatuser = listOf("AVLYST", "AVBRUTT", "AVSLUTT")
	private val ikkeStartetStatuser = listOf("PLANLAGT")
	private val gjennomforesStatuser = listOf("GJENNOMFOR")

	fun convert (arenaStatus: String) : GjennomforingStatus {
		return when (arenaStatus) {
			in avsluttendeStatuser -> GjennomforingStatus.AVSLUTTET
			in ikkeStartetStatuser -> GjennomforingStatus.IKKE_STARTET
			in gjennomforesStatuser -> GjennomforingStatus.GJENNOMFORES
			else -> throw DataIntegrityViolationException("Ukjent status fra arena: $arenaStatus")
		}

	}
}

package no.nav.amt.arena.acl.clients.mr_arena_adapter

import java.time.LocalDate
import java.util.*

interface MrArenaAdapterClient {

	fun hentGjennomforing(id: UUID): Gjennomforing

	fun hentGjennomforingId(arenaId: String): UUID?

	fun hentGjennomforingArenaData(id: UUID): GjennomforingArenaData

}

data class GjennomforingArenaData(
	val opprettetAar: Int,
	val lopenr: Int,
	val virksomhetsnummer: String,
	val ansvarligNavEnhetId: String,
	val status: String,
)

data class Gjennomforing(
	val id: UUID,
	val tiltak: Tiltakstype,
	val navn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
)

data class Tiltakstype(
	val id: UUID,
	val navn: String,
	val arenaKode: String,
)
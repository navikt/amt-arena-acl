package no.nav.amt.arena.acl.clients.mr_arena_adapter

import java.util.*

interface MrArenaAdapterClient {

	fun hentGjennomforingArenaData(id: UUID): GjennomforingArenaData

}

data class GjennomforingArenaData(
	val opprettetAar: Int,
	val lopenr: Int,
	val virksomhetsnummer: String,
	val ansvarligNavEnhetId: String,
	val status: String,
)

package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import java.util.*

interface MulighetsrommetApiClient {

	fun hentGjennomforingId(arenaId: String): UUID?

	fun hentGjennomforingArenaData(id: UUID): GjennomforingArenaData

}

data class GjennomforingArenaData(
	val opprettetAar: Int,
	val lopenr: Int,
	val virksomhetsnummer: String?,
	val ansvarligNavEnhetId: String,
	val status: String,
)

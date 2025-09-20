package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import java.util.UUID

interface MulighetsrommetApiClient {
	fun hentGjennomforingId(arenaId: String): UUID?

	fun hentGjennomforing(id: UUID): Gjennomforing
}


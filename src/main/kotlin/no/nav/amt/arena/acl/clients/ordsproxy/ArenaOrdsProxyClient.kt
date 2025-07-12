package no.nav.amt.arena.acl.clients.ordsproxy

interface ArenaOrdsProxyClient {
	fun hentFnr(arenaPersonId: String): String?

	fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String
}

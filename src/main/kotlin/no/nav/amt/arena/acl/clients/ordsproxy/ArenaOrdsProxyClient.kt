import no.nav.amt.arena.acl.clients.ordsproxy.Arbeidsgiver

interface ArenaOrdsProxyClient {

	fun hentFnr(arenaPersonId: String): String?
	fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String
}

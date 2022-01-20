import no.nav.amt.arena.acl.ordsproxy.Arbeidsgiver

interface ArenaOrdsProxyClient {

	fun hentFnr(arenaPersonId: String): String?
	fun hentArbeidsgiver(arenaArbeidsgiverId: String): Arbeidsgiver?
	fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String
}

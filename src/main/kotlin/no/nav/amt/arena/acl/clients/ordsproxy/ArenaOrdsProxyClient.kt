interface ArenaOrdsProxyClient {

	fun hentFnr(arenaPersonId: String): String?
	fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String
}

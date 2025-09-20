package no.nav.amt.arena.acl.clients.amttiltak

fun interface AmtTiltakClient {
	fun hentDeltakelserForPerson(personIdent: String): List<DeltakerDto>
}

package no.nav.amt.arena.acl.clients.amttiltak

import java.time.LocalDate
import java.util.UUID

interface AmtTiltakClient {
	fun hentDeltakelserForPerson(personIdent: String): List<DeltakerDto>
}

data class DeltakerDto(
	val id: UUID,
	val gjennomforing: GjennomforingDto,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
)

data class GjennomforingDto(
	val id: UUID
)

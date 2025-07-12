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
	val status: DeltakerStatusDto,
)

data class GjennomforingDto(
	val id: UUID,
)

enum class DeltakerStatusDto {
	UTKAST_TIL_PAMELDING,
	AVBRUTT_UTKAST,
	VENTER_PA_OPPSTART,
	DELTAR,
	HAR_SLUTTET,
	FULLFORT,
	IKKE_AKTUELL,
	FEILREGISTRERT,
	SOKT_INN,
	VURDERES,
	VENTELISTE,
	AVBRUTT,
	PABEGYNT_REGISTRERING,
}

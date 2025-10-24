package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing.Tiltakstype
import java.time.LocalDate
import java.util.UUID

interface MulighetsrommetApiClient {

	fun hentGjennomforingId(arenaId: String): UUID?

	fun hentGjennomforing(id: UUID): Gjennomforing

	fun hentGjennomforingV2(id: UUID): Gjennomforing
}


data class GjennomforingV2Response(
	val id: UUID,
	val tiltakstype: TiltakstypeResponse,
	val arrangor: ArrangorResponse,
) {
	data class ArrangorResponse(
		val organisasjonsnummer: String
	)

	data class TiltakstypeResponse(
		val tiltakskode: String,
		val arenakode: String
	)
	fun toGjennomforing() = Gjennomforing(
		id = id,
		tiltakstype = Tiltakstype(arenaKode = tiltakstype.arenakode),
		virksomhetsnummer = arrangor.organisasjonsnummer,
	)
}

data class Gjennomforing(
	val id: UUID,
	val tiltakstype: Tiltakstype,
	val navn: String? = null,
	val startDato: LocalDate? = null,
	val sluttDato: LocalDate? = null,
	val status: Status? = null,
	val virksomhetsnummer: String,
	val oppstart: Oppstartstype? = null,
) {
	enum class Oppstartstype {
		LOPENDE,
		FELLES
	}

	data class Tiltakstype(
		val id: UUID? = null,
		val navn: String? = null,
		val arenaKode: String
	)

	enum class Status {
		GJENNOMFORES,
		AVBRUTT,
		AVLYST,
		AVSLUTTET;
	}

	fun erAvsluttet(): Boolean {
		return status in listOf(Status.AVSLUTTET, Status.AVBRUTT, Status.AVLYST)

	}

	fun erKurs(): Boolean {
		return oppstart == Oppstartstype.FELLES
	}

	fun erEnkelplass(): Boolean {
		return tiltakstype.arenaKode in setOf("ENKELAMO", "ENKFAGYRKE", "HOYEREUTD")
	}
}

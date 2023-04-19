package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import no.nav.amt.arena.acl.services.KURS_TILTAK
import java.time.LocalDate
import java.util.*

interface MulighetsrommetApiClient {

	fun hentGjennomforingId(arenaId: String): UUID?

	fun hentGjennomforing(id: UUID): Gjennomforing

}

data class Gjennomforing (
	val id: UUID,
	val tiltakstype: Tiltakstype,
	val navn: String,
	val startDato: LocalDate,
	val sluttDato: LocalDate? = null,
	val status: Status,
	val virksomhetsnummer: String
) {

	data class Tiltakstype(
		val id: UUID,
		val navn: String,
		val arenaKode: String
	)

	enum class Status {
		GJENNOMFORES,
		AVBRUTT,
		AVLYST,
		AVSLUTTET,
		APENT_FOR_INNSOK;
	}

	fun erAvsluttet(): Boolean {
		return status in listOf(Status.AVSLUTTET, Status.AVBRUTT, Status.AVLYST)

	}

	fun erKurs() : Boolean {
		return KURS_TILTAK.contains(tiltakstype.arenaKode)
	}
}

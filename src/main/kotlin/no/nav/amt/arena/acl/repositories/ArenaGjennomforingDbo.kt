package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.AmtTiltak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class ArenaGjennomforingDbo(
	val id: UUID,
	val arenaSakId: Long?,
	val tiltakKode: String,
	val virksomhetsnummer: String,
	val navn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val registrertDato: LocalDateTime,
	val fremmoteDato: LocalDateTime?,
	val status: AmtGjennomforing.Status,

	val ansvarligNavEnhetId: String?,
	val opprettetAar: Int?,
	val lopenr: Int?
) {
	fun toAmtGjennomforing(tiltak: AmtTiltak) = AmtGjennomforing(
		id = id,
		tiltak = tiltak,
		virksomhetsnummer = virksomhetsnummer,
		navn = navn,
		startDato = startDato,
		sluttDato = sluttDato,
		registrertDato = registrertDato,
		fremmoteDato = fremmoteDato,
		status = status,
		ansvarligNavEnhetId = ansvarligNavEnhetId,
		opprettetAar = opprettetAar,
		lopenr = lopenr,
	)
}

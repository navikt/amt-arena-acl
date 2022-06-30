package no.nav.amt.arena.acl.domain.kafka.amt

import no.nav.amt.arena.acl.repositories.ArenaGjennomforingDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class AmtGjennomforing(
	val id: UUID,
	val tiltak: AmtTiltak,
	val virksomhetsnummer: String,
	val navn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val registrertDato: LocalDateTime,
	val fremmoteDato: LocalDateTime?,
	val status: Status,

	// Hentet fra Sak
	val ansvarligNavEnhetId: String?,
	val opprettetAar: Int?,
	val lopenr: Int?,
) {
	enum class Status {
		IKKE_STARTET,
		GJENNOMFORES,
		AVSLUTTET
	}

	fun toInsertDbo(sakId: Long?) = ArenaGjennomforingDbo (
		 id = id,
			arenaSakId = sakId,
			tiltakKode = tiltak.kode,
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

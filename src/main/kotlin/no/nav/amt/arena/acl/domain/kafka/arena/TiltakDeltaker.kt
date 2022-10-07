package no.nav.amt.arena.acl.domain.kafka.arena


import java.time.LocalDate
import java.time.LocalDateTime

data class TiltakDeltaker(
	val tiltakdeltakerId: String,
	val tiltakgjennomforingId: String,
	val personId: String,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val deltakerStatusKode: Status,
	val datoStatusendring: LocalDateTime?,
	val dagerPerUke: Int?,
	val prosentDeltid: Float?,
	val regDato: LocalDateTime,
	val innsokBegrunnelse: String?
) {
	enum class Status {
		DELAVB,    // Deltakelse avbrutt
		FULLF,     // Fullført
		GJENN_AVB, // Gjennomføring avbrutt
		GJENN_AVL, // Gjennomføring avlyst
		IKKEM,     // Ikke møtt
		IKKAKTUELL, // Ikke aktuell
		AVSLAG,     // Fått avslag
		NEITAKK,    // Takket nei til tilbud
		GJENN,       // Gjennomføres
		TILBUD,      // Godkjent tiltaksplass
		VENTELISTE,
		AKTUELL,
		JATAKK,
		INFOMOETE
	}

}

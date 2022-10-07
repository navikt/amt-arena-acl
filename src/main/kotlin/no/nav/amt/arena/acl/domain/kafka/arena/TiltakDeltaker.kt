package no.nav.amt.arena.acl.domain.kafka.arena

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.processors.converters.ArenaDeltakerStatusConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class TiltakDeltaker(
	val tiltakdeltakerId: String,
	val tiltakgjennomforingId: String,
	val personId: String,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val deltakerStatusKode: Status,
	val datoStatusendring: LocalDateTime?,
	val statusAarsakKode: StatusAarsak?,
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

	enum class StatusAarsak {
		HENLU, // Henlagt etter utredning
		SYK, // syk
		FRISM,  // Friskmeldt
		ANN, //Annet
		BEGA, //Begynt i arbeid
		UTV,  // Utvist
		FTOAT, // Fått tilbud om annet tiltak
	}

	fun toAmtDeltaker(
		amtDeltakerId: UUID,
		gjennomforingId: UUID,
		personIdent: String
	): AmtDeltaker {
		val statusConverter = ArenaDeltakerStatusConverter(
			deltakerRegistrertDato = regDato,
			startDato = datoFra,
			sluttDato = datoTil,
			deltakerStatusKode = deltakerStatusKode,
			datoStatusEndring = datoStatusendring?.toLocalDate(),
		)

		return AmtDeltaker(
			id = amtDeltakerId,
			gjennomforingId = gjennomforingId,
			personIdent = personIdent,
			startDato = datoFra,
			sluttDato = datoTil,
			statusAarsak = null,
			dagerPerUke = dagerPerUke,
			prosentDeltid = prosentDeltid,
			registrertDato = regDato,
			status = statusConverter.getStatus(),
			statusEndretDato = statusConverter.getEndretDato(),
			innsokBegrunnelse = innsokBegrunnelse
		)
	}
}

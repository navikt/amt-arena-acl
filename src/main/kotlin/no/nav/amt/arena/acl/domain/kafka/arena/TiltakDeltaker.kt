package no.nav.amt.arena.acl.domain.kafka.arena

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.processors.converters.ArenaDeltakerAarsakConverter
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
	val statusAarsakKode: StatusAarsak?,
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

	enum class StatusAarsak {
		HENLU, // Henlagt etter utredning
		SYK, // syk
		FRISM,  // Friskmeldt
		ANN, //Annet
		BEGA, //Begynt i arbeid
		UTV,  // Utvist
		FTOAT, // Fått tilbud om annet tiltak
	}

	fun constructDeltaker(
		amtDeltakerId: UUID,
		gjennomforingId: UUID,
		gjennomforingStatus: AmtGjennomforing.Status,
		personIdent: String
	): AmtDeltaker {
		val deltakerStatus = ArenaDeltakerStatusConverter.convert(
			deltakerRegistrertDato = regDato,
			startDato = datoFra,
			sluttDato = datoTil,
			deltakerStatusKode = deltakerStatusKode,
			statusEndringTid = datoStatusendring,
			gjennomforingStatus = gjennomforingStatus
		)
		val statusAarsak = ArenaDeltakerAarsakConverter.convert(
			deltakerStatusKode,
			deltakerStatus.navn,
			statusAarsakKode,
			gjennomforingStatus
		)

		return AmtDeltaker(
			id = amtDeltakerId,
			gjennomforingId = gjennomforingId,
			personIdent = personIdent,
			startDato = datoFra,
			sluttDato = datoTil,
			statusAarsak = statusAarsak,
			dagerPerUke = dagerPerUke,
			prosentDeltid = prosentDeltid,
			registrertDato = regDato,
			status = deltakerStatus.navn,
			statusEndretDato = deltakerStatus.endretDato,
			innsokBegrunnelse = innsokBegrunnelse
		)
	}
}

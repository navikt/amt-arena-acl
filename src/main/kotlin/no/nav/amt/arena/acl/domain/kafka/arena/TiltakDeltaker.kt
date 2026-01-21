package no.nav.amt.arena.acl.domain.kafka.arena

import no.nav.amt.arena.acl.consumer.converters.ArenaDeltakerAarsakConverter
import no.nav.amt.arena.acl.consumer.converters.ArenaDeltakerStatusConverter
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TiltakDeltaker(
	val tiltakdeltakerId: String,
	val tiltakgjennomforingId: String,
	val personId: String,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val statusAarsakKode: StatusAarsak?,
	val deltakerStatusKode: Status,
	val datoStatusendring: LocalDateTime?,
	val dagerPerUke: Float?,
	val prosentDeltid: Float?,
	val regDato: LocalDateTime,
	val innsokBegrunnelse: String?,
) {
	enum class Status {
		DELAVB, // Deltakelse avbrutt
		FULLF, // Fullført
		GJENN_AVB, // Gjennomføring avbrutt
		GJENN_AVL, // Gjennomføring avlyst
		IKKEM, // Ikke møtt
		IKKAKTUELL, // Ikke aktuell
		AVSLAG, // Fått avslag
		NEITAKK, // Takket nei til tilbud
		GJENN, // Gjennomføres
		TILBUD, // Godkjent tiltaksplass
		VENTELISTE,
		AKTUELL,
		JATAKK,
		INFOMOETE,
		FEILREG,
	}

	enum class StatusAarsak {
		HENLU, // Henlagt etter utredning
		SYK, // syk
		FRISM, // Friskmeldt
		ANN, // Annet
		BEGA, // Begynt i arbeid
		UTV, // Utvist
		FTOAT, // Fått tilbud om annet tiltak
	}

	fun constructDeltaker(
		amtDeltakerId: UUID,
		gjennomforingId: UUID,
		gjennomforingSluttDato: LocalDate?,
		erGjennomforingAvsluttet: Boolean,
		personIdent: String,
		deltakelseKreverGodkjenningLoep: Boolean,
	): AmtDeltaker {
		val deltakerStatus =
			ArenaDeltakerStatusConverter(
				deltakerRegistrertDato = regDato,
				deltakerStartdato = datoFra,
				deltakerSluttdato = datoTil,
				arenaStatus = deltakerStatusKode,
				datoStatusEndring = datoStatusendring,
				gjennomforingSluttdato = gjennomforingSluttDato,
				erGjennomforingAvsluttet = erGjennomforingAvsluttet,
				deltakelseKreverGodkjenningLoep = deltakelseKreverGodkjenningLoep,
			).convert()
		val statusAarsak =
			ArenaDeltakerAarsakConverter.convert(
				deltakerStatusKode,
				deltakerStatus.navn,
				statusAarsakKode,
				erGjennomforingAvsluttet,
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
			innsokBegrunnelse = innsokBegrunnelse,
		)
	}
}

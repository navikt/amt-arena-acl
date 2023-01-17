package no.nav.amt.arena.acl.processors.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.erAvsluttende
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object ArenaDeltakerStatusConverter {
	private val avsluttendeStatuser = listOf(
		TiltakDeltaker.Status.DELAVB,
		TiltakDeltaker.Status.FULLF,
		TiltakDeltaker.Status.GJENN_AVB,
		TiltakDeltaker.Status.GJENN_AVL,
		TiltakDeltaker.Status.IKKEM,
	)
	private val ikkeAktuelleStatuser = listOf(
		TiltakDeltaker.Status.IKKAKTUELL,
		TiltakDeltaker.Status.AVSLAG,
		TiltakDeltaker.Status.NEITAKK
	)
	private val gjennomforendeStatuser = listOf(TiltakDeltaker.Status.GJENN, TiltakDeltaker.Status.TILBUD)
	private val utkastStatuser = listOf(
		TiltakDeltaker.Status.VENTELISTE,
		TiltakDeltaker.Status.AKTUELL,
		TiltakDeltaker.Status.JATAKK,
		TiltakDeltaker.Status.INFOMOETE
	)

	private fun statusEndretSammeDagSomRegistrering(statusEndringTid: LocalDateTime?, deltakerRegistrertDato: LocalDateTime) =
		statusEndringTid != null && statusEndringTid.toLocalDate() == deltakerRegistrertDato.toLocalDate()

	private fun starterIDag(startDato: LocalDate?) = startDato?.equals(LocalDate.now()) == true

	private fun startDatoPassert(startDato: LocalDate?) = startDato?.isBefore(LocalDate.now()) ?: false

	private fun sluttDatoPassert(sluttDato: LocalDate?) = sluttDato?.isBefore(LocalDate.now()) ?: false

	private fun endretEtterStartDato(startDato: LocalDate?, statusEndringTid: LocalDateTime?) =
		startDato != null && statusEndringTid?.toLocalDate()?.isAfter(startDato) ?: false

	private fun sluttDatoHaddePassert(sluttDato: LocalDate?, statusEndringTid: LocalDateTime?)
		= statusEndringTid != null && sluttDato?.isBefore(statusEndringTid.toLocalDate()) ?: false

	private fun kanskjeFeilregistrert(statusEndringTid: LocalDateTime?, deltakerRegistrertDato: LocalDateTime): DeltakerStatus {
		return if (statusEndretSammeDagSomRegistrering(statusEndringTid, deltakerRegistrertDato))
			DeltakerStatus(AmtDeltaker.Status.FEILREGISTRERT, statusEndringTid)
		else
			DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, statusEndringTid)
	}

	private fun alltidIkkeAktuell(statusEndringTid: LocalDateTime?) = DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, statusEndringTid)

	private fun gjennomforendeStatus(startDato: LocalDate?, sluttDato: LocalDate?, statusEndringTid: LocalDateTime?): DeltakerStatus {
		return if (startDatoPassert(startDato) && sluttDatoPassert(sluttDato))
			DeltakerStatus(AmtDeltaker.Status.HAR_SLUTTET, sluttDato?.atStartOfDay())
		else if (starterIDag(startDato) || startDatoPassert(startDato))
			DeltakerStatus(AmtDeltaker.Status.DELTAR, startDato?.atStartOfDay())
		else DeltakerStatus(AmtDeltaker.Status.VENTER_PA_OPPSTART, statusEndringTid)
	}

	private fun avsluttendeStatus(startDato: LocalDate?, sluttDato: LocalDate?, statusEndringTid: LocalDateTime?): DeltakerStatus {
		return if (endretEtterStartDato(startDato, statusEndringTid) && sluttDatoHaddePassert(sluttDato, statusEndringTid))
			DeltakerStatus(AmtDeltaker.Status.HAR_SLUTTET, sluttDato?.atStartOfDay())
		else if (endretEtterStartDato(startDato, statusEndringTid))
			DeltakerStatus(AmtDeltaker.Status.HAR_SLUTTET, statusEndringTid)
		else
			DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, statusEndringTid)
	}

	private fun ventendeStatus(statusEndringTid: LocalDateTime?) = DeltakerStatus(AmtDeltaker.Status.PABEGYNT, statusEndringTid)

	fun convert(
		deltakerStatusKode: TiltakDeltaker.Status,
		deltakerRegistrertDato: LocalDateTime,
		startDato: LocalDate?,
		sluttDato: LocalDate?,
		statusEndringTid: LocalDateTime?,
		gjennomforingStatus: GjennomforingStatus,
	): DeltakerStatus {
		val status = if (deltakerStatusKode in avsluttendeStatuser) avsluttendeStatus(startDato, sluttDato, statusEndringTid)
				else if (deltakerStatusKode in gjennomforendeStatuser) gjennomforendeStatus(startDato, sluttDato, statusEndringTid)
				else if (deltakerStatusKode in utkastStatuser) ventendeStatus(statusEndringTid)
				else if (deltakerStatusKode in ikkeAktuelleStatuser) {
					if (deltakerStatusKode == TiltakDeltaker.Status.IKKAKTUELL) kanskjeFeilregistrert(statusEndringTid, deltakerRegistrertDato)
					else alltidIkkeAktuell(statusEndringTid)
				} else throw UnknownFormatConversionException("Kan ikke konvertere deltakerstatuskode: $deltakerStatusKode")

		if(gjennomforingStatus == GjennomforingStatus.AVSLUTTET && !status.navn.erAvsluttende()) {
			return DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, statusEndringTid)
		}
		return status
	}
}

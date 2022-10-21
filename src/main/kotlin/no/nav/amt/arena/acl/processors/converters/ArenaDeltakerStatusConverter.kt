package no.nav.amt.arena.acl.processors.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.processors.DeltakerStatusProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal data class ArenaDeltakerStatusConverter(
	private val deltakerStatusKode: TiltakDeltaker.Status,
	private val deltakerRegistrertDato: LocalDateTime,
	private val startDato: LocalDate?,
	private val sluttDato: LocalDate?,
	private val datoStatusEndring: LocalDate?,
) : DeltakerStatusProvider {

	private fun statusEndretSammeDagSomRegistrering() = datoStatusEndring != null && datoStatusEndring == deltakerRegistrertDato.toLocalDate()
	private fun starterIDag () = startDato?.equals(LocalDate.now()) == true
	private fun startDatoPassert() = startDato?.isBefore(LocalDate.now()) ?: false
	private fun sluttDatoPassert() = sluttDato?.isBefore(LocalDate.now()) ?: false
	private fun endretEtterStartDato() = startDato != null && datoStatusEndring?.isAfter(startDato) ?: false
	private fun sluttDatoHaddePassert() = datoStatusEndring != null && sluttDato?.isBefore(datoStatusEndring) ?: false
	private val status: Status

	val avsluttendeStatuser = listOf(
		TiltakDeltaker.Status.DELAVB,
		TiltakDeltaker.Status.FULLF,
		TiltakDeltaker.Status.GJENN_AVB,
		TiltakDeltaker.Status.GJENN_AVL,
		TiltakDeltaker.Status.IKKEM,
	)
	val ikkeAktuelleStatuser = listOf(
		TiltakDeltaker.Status.IKKAKTUELL,
		TiltakDeltaker.Status.AVSLAG,
		TiltakDeltaker.Status.NEITAKK
	)
	val gjennomforendeStatuser = listOf(TiltakDeltaker.Status.GJENN, TiltakDeltaker.Status.TILBUD)
	val utkastStatuser = listOf(
		TiltakDeltaker.Status.VENTELISTE,
		TiltakDeltaker.Status.AKTUELL,
		TiltakDeltaker.Status.JATAKK,
		TiltakDeltaker.Status.INFOMOETE
	)

	private val kanskjeFeilregistrert: () -> Status = {
		if (statusEndretSammeDagSomRegistrering())
			Status(AmtDeltaker.Status.FEILREGISTRERT, datoStatusEndring)
		else
			Status(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
	}

	private val alltidIkkeAktuell: () -> Status = {
		Status(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
	}

	private val gjennomforendeStatus: () -> Status = {
		if (startDatoPassert() && sluttDatoPassert())
			Status(AmtDeltaker.Status.HAR_SLUTTET, sluttDato)
		else if (starterIDag() || startDatoPassert())
			Status(AmtDeltaker.Status.DELTAR, startDato)
		else Status(AmtDeltaker.Status.VENTER_PA_OPPSTART, datoStatusEndring)
	}

	private val avsluttendeStatus:() -> Status = {
		if (endretEtterStartDato() && sluttDatoHaddePassert())
			Status(AmtDeltaker.Status.HAR_SLUTTET, sluttDato)
		else if (endretEtterStartDato())
			Status(AmtDeltaker.Status.HAR_SLUTTET, datoStatusEndring)
		else
			Status(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
	}

	private val ventendeStatus: () -> Status = {
		Status(AmtDeltaker.Status.PABEGYNT, datoStatusEndring)
	}

	private fun utledStatus (): Status {
		if (deltakerStatusKode in avsluttendeStatuser) return avsluttendeStatus()
		else if (deltakerStatusKode in gjennomforendeStatuser) return gjennomforendeStatus()
		else if (deltakerStatusKode in utkastStatuser) return ventendeStatus()
		else if (deltakerStatusKode in ikkeAktuelleStatuser) {
			if (deltakerStatusKode == TiltakDeltaker.Status.IKKAKTUELL) return kanskjeFeilregistrert()
			else return alltidIkkeAktuell()
		}
		throw UnknownFormatConversionException("Kan ikke konvertere deltakerstatuskode: $deltakerStatusKode")
	}

	init {
		status = utledStatus()
	}

	override fun getStatus () : AmtDeltaker.Status {
		return status.navn
	}

	override fun getEndretDato () : LocalDateTime? {
		return status.endretDato?.atStartOfDay()
	}

}

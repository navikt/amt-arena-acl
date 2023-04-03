package no.nav.amt.arena.acl.processors.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.erAvsluttende
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArenaDeltakerStatusConverter(
	val arenaStatus: TiltakDeltaker.Status,
	val deltakerRegistrertDato: LocalDateTime,
	val deltakerStartdato: LocalDate?,
	val deltakerSluttdato: LocalDate?,
	val datoStatusEndring: LocalDateTime?,
	val erGjennomforingAvsluttet: Boolean,
	val gjennomforingSluttdato: LocalDate?,
	val erKurs: Boolean
) {

	fun convert(): DeltakerStatus {
		val status =
			if (erKurs) convertKursStatuser()
			else if (arenaStatus.erSoktInn()) DeltakerStatus(AmtDeltaker.Status.PABEGYNT_REGISTRERING, datoStatusEndring)
			else if (arenaStatus.erGjennomforende()) utledGjennomforendeStatus()
			else if (arenaStatus.erAvsluttende()) utledAvsluttendeStatus()
			else if (arenaStatus.erIkkeAktuell()) utledIkkeAkuelleStatus()
			else throw UnknownFormatConversionException("Kan ikke konvertere deltakerstatuskode: $arenaStatus")

		if (erGjennomforingAvsluttet && !status.navn.erAvsluttende()) {
			return DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
		}
		return status
	}

	private fun convertKursStatuser() : DeltakerStatus {
		val status: DeltakerStatus

		if (arenaStatus.erSoktInn()) status = utledSoktInnStatus()

		else if (arenaStatus.erGjennomforende()) {
			if (startDatoHarPassert() && sluttDatoHarPassert() && sluttetForTidlig()) {
				status = DeltakerStatus(AmtDeltaker.Status.AVBRUTT, deltakerSluttdato?.atStartOfDay())
			}
			else if (utledGjennomforendeStatus().navn == AmtDeltaker.Status.VENTER_PA_OPPSTART) {
				status = DeltakerStatus(AmtDeltaker.Status.FATT_PLASS, deltakerSluttdato?.atStartOfDay())
			}
			else status = utledGjennomforendeStatus()
		}
		else if (arenaStatus == TiltakDeltaker.Status.FULLF) {
			if (sluttetForTidlig()) status = utledAvbruttStatus()
			else status = utledAvsluttendeStatus()
		}
		else if (arenaStatus.erAvsluttende()) {
			status = utledAvbruttStatus()
		}

		else if (arenaStatus.erIkkeAktuell()) {
			status = utledIkkeAkuelleStatus()
		}

		else throw UnknownFormatConversionException("Kan ikke konvertere deltakerstatuskode: $arenaStatus")

		return status
	}

	private fun statusEndretSammeDagSomRegistrering() = datoStatusEndring != null && datoStatusEndring.toLocalDate() == deltakerRegistrertDato.toLocalDate()

	private fun sluttetForTidlig() = deltakerSluttdato?.isBefore(gjennomforingSluttdato) == true

	private fun starterIDag() = deltakerStartdato?.equals(LocalDate.now()) == true

	private fun startDatoHarPassert() = deltakerStartdato?.isBefore(LocalDate.now()) ?: false

	private fun sluttDatoHarPassert() = deltakerSluttdato?.isBefore(LocalDate.now()) ?: false

	private fun statusEndretEtterStartDato() =
		deltakerStartdato != null && datoStatusEndring?.isAfter(deltakerStartdato.atStartOfDay()) ?: false

	private fun sluttDatoHaddePassert() =
		datoStatusEndring != null && deltakerSluttdato?.atStartOfDay()?.isBefore(datoStatusEndring) ?: false


	private fun utledGjennomforendeStatus(): DeltakerStatus {
		if (startDatoHarPassert() && sluttDatoHarPassert())
			return DeltakerStatus(AmtDeltaker.Status.HAR_SLUTTET, deltakerSluttdato?.atStartOfDay())
		else if (starterIDag() || startDatoHarPassert())
			return DeltakerStatus(AmtDeltaker.Status.DELTAR, deltakerStartdato?.atStartOfDay())
		else return DeltakerStatus(AmtDeltaker.Status.VENTER_PA_OPPSTART, datoStatusEndring)
	}

	private fun utledSoktInnStatus(): DeltakerStatus {
		return when (arenaStatus) {
			TiltakDeltaker.Status.AKTUELL -> DeltakerStatus(AmtDeltaker.Status.SOKT_INN, datoStatusEndring)
			TiltakDeltaker.Status.INFOMOETE -> DeltakerStatus(AmtDeltaker.Status.VURDERES, datoStatusEndring)
			TiltakDeltaker.Status.VENTELISTE -> DeltakerStatus(AmtDeltaker.Status.VENTELISTE, datoStatusEndring)
			else -> throw IllegalStateException("Fant ikke status ${arenaStatus.name}")
		}
	}

	private fun utledAvsluttendeStatus(): DeltakerStatus {
		if (statusEndretEtterStartDato()) {
			val dato = if (sluttDatoHaddePassert()) deltakerSluttdato?.atStartOfDay() else datoStatusEndring
			return DeltakerStatus(AmtDeltaker.Status.HAR_SLUTTET, dato)
		} else {
			return DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
		}
	}

	private fun utledAvbruttStatus(): DeltakerStatus {
		if (statusEndretEtterStartDato()) {
			val dato = if (sluttDatoHaddePassert()) deltakerSluttdato?.atStartOfDay() else datoStatusEndring
			return  DeltakerStatus(AmtDeltaker.Status.AVBRUTT, dato)
		} else {
			return DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
		}
	}
	private fun utledIkkeAkuelleStatus(): DeltakerStatus {
		if (arenaStatus == TiltakDeltaker.Status.IKKAKTUELL
			&& statusEndretSammeDagSomRegistrering()) {
			return DeltakerStatus(AmtDeltaker.Status.FEILREGISTRERT, datoStatusEndring)
		}
		else return DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
	}


	private fun TiltakDeltaker.Status.erAvsluttende(): Boolean {
		return this in listOf(
			TiltakDeltaker.Status.DELAVB,
			TiltakDeltaker.Status.FULLF,
			TiltakDeltaker.Status.GJENN_AVB,
			TiltakDeltaker.Status.GJENN_AVL,
			TiltakDeltaker.Status.IKKEM,
		)
	}

	private fun TiltakDeltaker.Status.erIkkeAktuell(): Boolean {
		return this in listOf(
			TiltakDeltaker.Status.IKKAKTUELL,
			TiltakDeltaker.Status.AVSLAG,
			TiltakDeltaker.Status.NEITAKK
		)
	}

	private fun TiltakDeltaker.Status.erGjennomforende(): Boolean {
		return this in listOf(
			TiltakDeltaker.Status.GJENN,
			TiltakDeltaker.Status.TILBUD,
			TiltakDeltaker.Status.JATAKK
		)
	}

	private fun TiltakDeltaker.Status.erSoktInn(): Boolean {
		return this in listOf(
			TiltakDeltaker.Status.VENTELISTE,
			TiltakDeltaker.Status.AKTUELL,
			TiltakDeltaker.Status.INFOMOETE
		)
	}

}

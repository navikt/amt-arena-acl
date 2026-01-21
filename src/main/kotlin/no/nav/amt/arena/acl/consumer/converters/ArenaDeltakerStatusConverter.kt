package no.nav.amt.arena.acl.consumer.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.erAvsluttende
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UnknownFormatConversionException

class ArenaDeltakerStatusConverter(
	val arenaStatus: TiltakDeltaker.Status,
	val deltakerRegistrertDato: LocalDateTime,
	val deltakerStartdato: LocalDate?,
	val deltakerSluttdato: LocalDate?,
	val datoStatusEndring: LocalDateTime?,
	val erGjennomforingAvsluttet: Boolean,
	val gjennomforingSluttdato: LocalDate?,
	val deltakelseKreverGodkjenningLoep: Boolean,
) {
	fun convert(): DeltakerStatus {
		val status =
			if (arenaStatus.erSoktInn()) {
				utledSoktInnStatus()
			} else if (arenaStatus.erFeilregistrert()) {
				utledFeilregistrertStatus()
			} else if (deltakelseKreverGodkjenningLoep) {
				convertKursStatuser()
			} else if (arenaStatus.erGjennomforende()) {
				utledGjennomforendeStatus()
			} else if (arenaStatus.erAvsluttende()) {
				utledAvsluttendeStatus()
			} else if (arenaStatus.erIkkeAktuell()) {
				utledIkkeAkuelleStatus()
			} else {
				throw UnknownFormatConversionException("Kan ikke konvertere deltakerstatuskode: $arenaStatus")
			}

		if (erGjennomforingAvsluttet && !status.navn.erAvsluttende()) {
			return DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
		}
		return status
	}

	private fun convertKursStatuser(): DeltakerStatus {
		val status: DeltakerStatus

		if (arenaStatus.erSoktInn()) {
			status = utledSoktInnStatus()
		} else if (arenaStatus.erGjennomforende()) {
			status =
				if (startDatoHarPassert() && sluttDatoHarPassert() && sluttetForTidlig()) {
					DeltakerStatus(AmtDeltaker.Status.AVBRUTT, deltakerSluttdato?.atStartOfDay())
				} else {
					utledGjennomforendeStatus()
				}
		} else if (arenaStatus == TiltakDeltaker.Status.FULLF) {
			status = utledAvsluttendeStatus()
		} else if (arenaStatus.erAvsluttende()) {
			status = utledAvbruttStatus()
		} else if (arenaStatus.erIkkeAktuell()) {
			status = utledIkkeAkuelleStatus()
		} else {
			throw UnknownFormatConversionException("Kan ikke konvertere deltakerstatuskode: $arenaStatus")
		}

		return status
	}

	private fun statusEndretSammeDagSomRegistrering() =
		datoStatusEndring != null && datoStatusEndring.toLocalDate() == deltakerRegistrertDato.toLocalDate()

	private fun sluttetForTidlig() = gjennomforingSluttdato != null && deltakerSluttdato?.isBefore(gjennomforingSluttdato) == true

	private fun starterIDag() = deltakerStartdato?.equals(LocalDate.now()) == true

	private fun startDatoHarPassert() = deltakerStartdato?.isBefore(LocalDate.now()) ?: false

	private fun sluttDatoHarPassert() = deltakerSluttdato?.isBefore(LocalDate.now()) ?: false

	private fun statusEndretEtterStartDato() =
		deltakerStartdato != null && datoStatusEndring?.isAfter(deltakerStartdato.atStartOfDay()) ?: false

	private fun sluttDatoHaddePassert() = datoStatusEndring != null && deltakerSluttdato?.atStartOfDay()?.isBefore(datoStatusEndring) ?: false

	private fun utledGjennomforendeStatus(): DeltakerStatus =
		if (startDatoHarPassert() && sluttDatoHarPassert()) {
			if (deltakelseKreverGodkjenningLoep) {
				DeltakerStatus(AmtDeltaker.Status.FULLFORT, deltakerSluttdato?.atStartOfDay())
			} else {
				DeltakerStatus(AmtDeltaker.Status.HAR_SLUTTET, deltakerSluttdato?.atStartOfDay())
			}
		} else if (starterIDag() || startDatoHarPassert()) {
			DeltakerStatus(AmtDeltaker.Status.DELTAR, deltakerStartdato?.atStartOfDay())
		} else {
			DeltakerStatus(AmtDeltaker.Status.VENTER_PA_OPPSTART, datoStatusEndring)
		}

	private fun utledSoktInnStatus(): DeltakerStatus =
		when (arenaStatus) {
			TiltakDeltaker.Status.AKTUELL -> DeltakerStatus(AmtDeltaker.Status.SOKT_INN, datoStatusEndring)
			TiltakDeltaker.Status.INFOMOETE -> DeltakerStatus(AmtDeltaker.Status.VURDERES, datoStatusEndring)
			TiltakDeltaker.Status.VENTELISTE -> DeltakerStatus(AmtDeltaker.Status.VENTELISTE, datoStatusEndring)
			else -> throw IllegalStateException("Fant ikke status ${arenaStatus.name}")
		}

	private fun utledAvsluttendeStatus(): DeltakerStatus {
		if (statusEndretEtterStartDato()) {
			val dato = if (sluttDatoHaddePassert()) deltakerSluttdato?.atStartOfDay() else datoStatusEndring
			return if (deltakelseKreverGodkjenningLoep) {
				DeltakerStatus(AmtDeltaker.Status.FULLFORT, dato)
			} else {
				DeltakerStatus(AmtDeltaker.Status.HAR_SLUTTET, dato)
			}
		} else {
			return DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
		}
	}

	private fun utledAvbruttStatus(): DeltakerStatus {
		if (statusEndretEtterStartDato()) {
			val dato = if (sluttDatoHaddePassert()) deltakerSluttdato?.atStartOfDay() else datoStatusEndring
			return DeltakerStatus(AmtDeltaker.Status.AVBRUTT, dato)
		} else {
			return DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
		}
	}

	private fun utledIkkeAkuelleStatus(): DeltakerStatus =
		if (arenaStatus == TiltakDeltaker.Status.IKKAKTUELL && statusEndretSammeDagSomRegistrering()) {
			DeltakerStatus(AmtDeltaker.Status.FEILREGISTRERT, datoStatusEndring)
		} else {
			DeltakerStatus(AmtDeltaker.Status.IKKE_AKTUELL, datoStatusEndring)
		}

	private fun utledFeilregistrertStatus(): DeltakerStatus = DeltakerStatus(AmtDeltaker.Status.FEILREGISTRERT, datoStatusEndring)

	private fun TiltakDeltaker.Status.erAvsluttende(): Boolean =
		this in
			listOf(
				TiltakDeltaker.Status.DELAVB,
				TiltakDeltaker.Status.FULLF,
				TiltakDeltaker.Status.GJENN_AVB,
				TiltakDeltaker.Status.GJENN_AVL,
				TiltakDeltaker.Status.IKKEM,
			)

	private fun TiltakDeltaker.Status.erIkkeAktuell(): Boolean =
		this in
			listOf(
				TiltakDeltaker.Status.IKKAKTUELL,
				TiltakDeltaker.Status.AVSLAG,
				TiltakDeltaker.Status.NEITAKK,
			)

	private fun TiltakDeltaker.Status.erGjennomforende(): Boolean =
		this in
			listOf(
				TiltakDeltaker.Status.GJENN,
				TiltakDeltaker.Status.TILBUD,
				TiltakDeltaker.Status.JATAKK,
			)

	private fun TiltakDeltaker.Status.erSoktInn(): Boolean =
		this in
			listOf(
				TiltakDeltaker.Status.VENTELISTE,
				TiltakDeltaker.Status.AKTUELL,
				TiltakDeltaker.Status.INFOMOETE,
			)

	private fun TiltakDeltaker.Status.erFeilregistrert(): Boolean = this == TiltakDeltaker.Status.FEILREG
}

package no.nav.amt.arena.acl.processors.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.processors.DeltakerStatusProvider
import java.time.LocalDate
import java.time.LocalDateTime


internal data class ArenaDeltakerStatusConverter(
	private val deltakerStatusKode: String,
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
	private fun sluttDatoHaddePassert() = datoStatusEndring != null && sluttDato?.isBefore(datoStatusEndring)?: false
	private val status: Status


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

	private val alleStatuser: Map<String, () -> Status> = mapOf(
		"DELAVB" to avsluttendeStatus, // Deltakelse avbrutt
		"FULLF" to avsluttendeStatus, // Fullført
		"GJENN_AVB" to avsluttendeStatus, // Gjennomføring avbrutt
		"GJENN_AVL" to avsluttendeStatus, // Gjennomføring avlyst
		"IKKEM" to avsluttendeStatus, // Ikke møtt

		"GJENN" to gjennomforendeStatus, // Gjennomføres
		"INFOMOETE" to gjennomforendeStatus, // Informasjonmøte
		"JATAKK" to gjennomforendeStatus, // Takket ja  til tilbud
		"VENTELISTE" to gjennomforendeStatus, // Venteliste
		"AKTUELL" to gjennomforendeStatus, // Aktuell
		"TILBUD" to gjennomforendeStatus, // Godkjent tiltaksplass

		"IKKAKTUELL" to kanskjeFeilregistrert, // Ikke aktuell
		"AVSLAG" to alltidIkkeAktuell, // Fått avslag
		"NEITAKK" to alltidIkkeAktuell, // Takket nei til tilbud

	)

	init {
	    status = alleStatuser.getValue(deltakerStatusKode)()
	}

	override fun getStatus () : AmtDeltaker.Status {
		return status.navn
	}

	override fun getEndretDato () : LocalDateTime? {
		return status.endretDato?.atStartOfDay()
	}


}

package no.nav.amt.arena.acl.processors.converters

import java.time.LocalDateTime


class DeltakerEndretDatoConverter {
	private val avsluttendeStatuser = listOf("FULLF", "DELAVB", "IKKEM", "GJENN_AVB", "GJENN_AVL")
//	private val ikkeAktuelleStatuser = listOf("IKKAKTUELL", "AVSLAG", "NEITAKK")
	private val gjennomforendeStatuser = listOf("TILBUD", "GJENN", "INFOMOETE", "JATAKK", "VENTELISTE", "AKTUELL", "TILBUD")

	internal fun convert(
		deltakerStatus : String,
		datoStatusEndring: LocalDateTime?,
		oppstartDato: LocalDateTime?,
		sluttDato: LocalDateTime?
	) : LocalDateTime? {
		val now = LocalDateTime.now()
		val harOppstartPassert = oppstartDato?.isBefore(now)?: false
		val harSluttDatoPassert = sluttDato?.isBefore(now)?: false

		val haddeStartDatoPassert = oppstartDato?.isBefore(datoStatusEndring)?: false
		val haddeSluttdatoPassert = sluttDato?.isBefore(datoStatusEndring)?: false

		if (deltakerStatus in avsluttendeStatuser) {
			if(!harSluttDatoPassert) return datoStatusEndring

			if (haddeStartDatoPassert
				&& !haddeSluttdatoPassert) return sluttDato
		}
		else if (deltakerStatus in gjennomforendeStatuser){
			if (harOppstartPassert
				&& harSluttDatoPassert) return sluttDato

			if (harOppstartPassert) return oppstartDato

		}
		return datoStatusEndring
	}
}

//HVis vi får en som er avsluttet og sluttdato hadde passert når den ble avsluttet, så skal vi bruke sluttdato
// Hvis vi får en som er avsluttet og

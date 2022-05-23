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
		val endretEtterOppstart = oppstartDato?.isBefore(datoStatusEndring)?: false

		if (deltakerStatus in avsluttendeStatuser
			&& endretEtterOppstart) return sluttDato // Konverterer vi til HAR_SLUTTA?

		else if (deltakerStatus in gjennomforendeStatuser) {
			if (harSluttDatoPassert) return sluttDato
			if (harOppstartPassert) return oppstartDato
		}

		return datoStatusEndring //Konverterer vi til IKKE_AKTUELL
	}
}


//finnes det avsluttende status som ikke har sluttdato?

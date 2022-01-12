package no.nav.amt.arena.acl.processors
import java.time.LocalDateTime

class DeltakerEndretDatoConverter {
	private val avsluttendeStatuser = listOf("FULLF", "DELAVB", "IKKEM", "GJENN_AVB", "GJENN_AVL")
	private val ikkeAktuelleStatuser = listOf("IKKAKTUELL", "AVSLAG", "NEITAKK")
	private val gjennomforendeStatuser = listOf("TILBUD", "GJENN", "INFOMOETE", "JATAKK", "VENTELISTE", "AKTUELL", "TILBUD")

	internal fun convert(
		deltakerStatus : String,
		datoStatusEndring: LocalDateTime?,
		oppstartDato: LocalDateTime?,
		sluttDato: LocalDateTime?
	) : LocalDateTime? {
		val comparator = if (deltakerStatus in gjennomforendeStatuser) LocalDateTime.now() else datoStatusEndring
		fun startDatoPassert () = comparator != null && oppstartDato?.isBefore(comparator)?: false
		fun sluttDatoPassert() = comparator != null && sluttDato?.isBefore(comparator)?: false

		if (deltakerStatus in avsluttendeStatuser) {
			if (sluttDato != null
				&& startDatoPassert()
				&& !sluttDatoPassert()) return sluttDato
		}
		if (deltakerStatus in gjennomforendeStatuser){
			if (sluttDato != null
				&& startDatoPassert()
				&& sluttDatoPassert()) return sluttDato

			if (oppstartDato != null
				&& startDatoPassert()
				&& !sluttDatoPassert()) return oppstartDato

		}
		return datoStatusEndring
	}
}

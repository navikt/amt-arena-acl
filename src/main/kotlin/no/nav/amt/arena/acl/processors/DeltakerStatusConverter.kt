package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.amt.AmtDeltaker
import org.springframework.stereotype.Component
import java.time.LocalDate

private typealias ConversionStrategy = (DateRange) -> AmtDeltaker.Status

@Component
open class DeltakerStatusConverter {

	private val alleStatuser: Map<String, ConversionStrategy> = mapOf(
		"AKTUELL" to { AmtDeltaker.Status.IKKE_AKTUELL }, // Aktuell
		"AVSLAG" to { AmtDeltaker.Status.IKKE_AKTUELL }, // Fått avslag
		"DELAVB" to { if (it.startDatoPassert()) AmtDeltaker.Status.HAR_SLUTTET else AmtDeltaker.Status.IKKE_AKTUELL }, // Deltakelse avbrutt
		"FULLF" to { AmtDeltaker.Status.HAR_SLUTTET }, // Fullført
		"GJENN" to
			{
				if (it.startDatoPassert() && it.sluttDatoPassert())
					AmtDeltaker.Status.HAR_SLUTTET
				else if (it.startDatoPassert())
					AmtDeltaker.Status.GJENNOMFORES
				else AmtDeltaker.Status.VENTER_PA_OPPSTART
			}, // Gjennomføres
		"GJENN_AVB" to { if (it.startDatoPassert()) AmtDeltaker.Status.HAR_SLUTTET else AmtDeltaker.Status.IKKE_AKTUELL }, // Gjennomføring avbrutt
		"GJENN_AVL" to { if (it.startDatoPassert()) AmtDeltaker.Status.HAR_SLUTTET else AmtDeltaker.Status.IKKE_AKTUELL }, // Gjennomføring avlyst
		"IKKAKTUELL" to { AmtDeltaker.Status.IKKE_AKTUELL }, // Ikke aktuell
		"IKKEM" to { if (it.startDatoPassert()) AmtDeltaker.Status.HAR_SLUTTET else AmtDeltaker.Status.IKKE_AKTUELL }, // Ikke møtt
		"INFOMOETE" to { AmtDeltaker.Status.IKKE_AKTUELL }, // Informasjonmøte
		"JATAKK" to { AmtDeltaker.Status.IKKE_AKTUELL }, // Takket ja  til tilbud
		"NEITAKK" to { AmtDeltaker.Status.IKKE_AKTUELL }, // Takket nei til tilbud
		"TILBUD" to { AmtDeltaker.Status.VENTER_PA_OPPSTART }, // Godkjent tiltaksplass
		"VENTELISTE" to { AmtDeltaker.Status.IKKE_AKTUELL } // Venteliste
	)

	internal fun convert(
		AmtDeltakerStatusCode: String?,
		startDato: LocalDate?,
		sluttDato: LocalDate?
	): AmtDeltaker.Status {
		requireNotNull(AmtDeltakerStatusCode) { "AmtDeltakerStatsKode kan ikke være null" }

		return alleStatuser.getValue(AmtDeltakerStatusCode)(DateRange(startDato, sluttDato))
	}

}

private data class DateRange(
	val start: LocalDate?,
	val end: LocalDate?
) {

	fun startDatoPassert() = start?.isBefore(LocalDate.now()) ?: false
	fun sluttDatoPassert() = end?.isBefore(LocalDate.now()) ?: false

}

package no.nav.amt.arena.acl.processors

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.*
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.processors.converters.ArenaDeltakerStatusConverter
import java.time.LocalDate
import java.time.LocalDateTime

private val tomorrow = LocalDate.now().plusDays(1)
private val yesterday = LocalDate.now().minusDays(1)
private val now = LocalDateTime.now()

/* Kjente statuser
	"AKTUELL", // Aktuell
	"AVSLAG", // Fått avslag
	"DELAVB", // Deltakelse avbrutt
	"FULLF", // Fullført
	"GJENN", // Gjennomføres
	"GJENN_AVB", // Gjennomføring avbrutt
	"GJENN_AVL", // Gjennomføring avlyst
	"IKKAKTUELL", // Ikke aktuell
	"IKKEM", // Ikke møtt
	"INFOMOETE", // Informasjonmøte
	"JATAKK", // Takket ja  til tilbud
	"NEITAKK", // Takket nei til tilbud
	"TILBUD", // Godkjent tiltaksplass
	"VENTELISTE" // Venteliste
*/


class DeltakerStatusConverterTest : StringSpec({

	"status - AKTUELL - returnerer PABEGYNT" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.AKTUELL, now, null, null, null).getStatus() shouldBe PABEGYNT
	}

	"status - AVSLAG og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.AVSLAG, now, null, null, null).getStatus() shouldBe IKKE_AKTUELL
	}

	"status - DELAVB og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.DELAVB, now, null, null, null).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.DELAVB, now, yesterday.minusDays(1), null, yesterday).getStatus() shouldBe HAR_SLUTTET
	}
	"status - DELAVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.DELAVB, now, yesterday, null, yesterday.minusDays(1)).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.DELAVB, now, tomorrow, null, null).getStatus() shouldBe IKKE_AKTUELL
	}

	"status - FULLF og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.FULLF, now, null, null, null).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - FULLF og har startdato før endretdato - returnerer HAR_SLUTTET" {
		val endretDato = yesterday
		val status = ArenaDeltakerStatusConverter(TiltakDeltaker.Status.FULLF, now, endretDato.minusDays(1), null, endretDato)
		status.getStatus() shouldBe HAR_SLUTTET
		status.getEndretDato() shouldBe endretDato.atStartOfDay()
	}
	"status - FULLF og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		val endretDato = yesterday.minusDays(1)

		val status = ArenaDeltakerStatusConverter(TiltakDeltaker.Status.FULLF, now, yesterday, null, endretDato)

		status.getStatus() shouldBe IKKE_AKTUELL
		status.getEndretDato() shouldBe endretDato.atStartOfDay()
	}
	"status - FULLF og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.FULLF, now, tomorrow, null, null).getStatus() shouldBe IKKE_AKTUELL
	}

	"status - FULLF og har endretdato etter sluttdato - returnerer HAR_SLUTTET" {
		val sluttDato = now.toLocalDate()

		val status = ArenaDeltakerStatusConverter(TiltakDeltaker.Status.FULLF, now, sluttDato.minusDays(1), sluttDato, sluttDato.plusDays(1))

		status.getStatus() shouldBe HAR_SLUTTET
		status.getEndretDato() shouldBe sluttDato.atStartOfDay()
	}

	"status - GJENN - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN, now, null, null, null).getStatus() shouldBe VENTER_PA_OPPSTART
	}
	"status - GJENN og har startdato i fortid - returnerer GJENNOMFORES" {
		val startDato = yesterday.minusDays(1)
		val status = ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN, now, startDato, null, startDato.plusDays(1))
		status.getStatus() shouldBe DELTAR
		status.getEndretDato() shouldBe startDato.atStartOfDay()
	}
	"status - GJENN og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		val datoEndret = tomorrow
		val status = ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN, now, datoEndret.plusDays(2), null, datoEndret)
		status.getStatus() shouldBe VENTER_PA_OPPSTART
		status.getEndretDato() shouldBe datoEndret.atStartOfDay()
	}
	"status - GJENN og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN, now, yesterday.minusDays(1), yesterday, null).getStatus() shouldBe HAR_SLUTTET
	}
	"status - GJENN og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		val startDato = yesterday
		val status = ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN, now, startDato, tomorrow.plusDays(1), tomorrow)
		status.getStatus() shouldBe DELTAR
		status.getEndretDato() shouldBe startDato.atStartOfDay()
	}
	"status - GJENN og har sluttdato har passert - returnerer HAR_SLUTTET" {
		val sluttDato = yesterday
		val status = ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN, now, sluttDato.minusDays(1), sluttDato, sluttDato.plusDays(1))
		status.getStatus() shouldBe HAR_SLUTTET
		status.getEndretDato() shouldBe sluttDato.atStartOfDay()
	}


	"status - GJENN_AVB og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN_AVB, now, null, null, null).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN_AVB, now, yesterday.minusDays(1), null, yesterday).getStatus() shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN_AVB, now, yesterday, null, yesterday.minusDays(1)).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN_AVB, now, tomorrow, null, null).getStatus() shouldBe IKKE_AKTUELL
	}


	"status - GJENN_AVL og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN_AVL, now, null, null, null).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN_AVL, now, yesterday.minusDays(1), null, yesterday).getStatus() shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVL og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN_AVL, now, yesterday, null, yesterday.minusDays(1)).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.GJENN_AVL, now, tomorrow, null, null).getStatus() shouldBe IKKE_AKTUELL
	}


	"status - IKKAKTUELL - returnerer IKKE_AKTUELL" {
		val statusEndret = yesterday.minusDays(5)
		val status = ArenaDeltakerStatusConverter(TiltakDeltaker.Status.IKKAKTUELL, now, statusEndret.plusDays(2), statusEndret.plusDays(4), statusEndret)
		status.getStatus() shouldBe IKKE_AKTUELL
		status.getEndretDato() shouldBe statusEndret.atStartOfDay()
	}

	"status - IKKAKTUELL - returnerer FEILREGISTRERT hvis registrert dato er samme dag som status endring" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.IKKAKTUELL, now, null, null, now.toLocalDate()).getStatus() shouldBe FEILREGISTRERT
	}


	"status - IKKEM og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.IKKEM, now, null, null, null).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.IKKEM, now, yesterday.minusDays(1), null, yesterday).getStatus() shouldBe HAR_SLUTTET
	}
	"status - IKKEM og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.IKKEM, now, yesterday, null, yesterday.minusDays(1)).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.IKKEM, now, tomorrow, null, null).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - INFOMOETE - returnerer PABEGYNT" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.INFOMOETE, now, null, null, null).getStatus() shouldBe PABEGYNT
	}
	"status - JATAKK - returnerer PABEGYNT" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.JATAKK, now, null, null, null).getStatus() shouldBe PABEGYNT
	}
	"status - NEITAKK - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.NEITAKK, now, null, null, null).getStatus() shouldBe IKKE_AKTUELL
	}
	"status - TILBUD - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.TILBUD, now, null, null, null).getStatus() shouldBe VENTER_PA_OPPSTART
	}
	"status - TILBUD og har startdato i fortid - returnerer GJENNOMFORES" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.TILBUD, now, yesterday, null, null).getStatus() shouldBe DELTAR
	}
	"status - TILBUD og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.TILBUD, now, tomorrow, null, null).getStatus() shouldBe VENTER_PA_OPPSTART
	}
	"status - TILBUD og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.TILBUD, now, yesterday.minusDays(1), yesterday, null).getStatus() shouldBe HAR_SLUTTET
	}
	"status - TILBUD og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.TILBUD, now, yesterday, tomorrow, null).getStatus() shouldBe DELTAR
	}
	"status - PABEGYNT - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(TiltakDeltaker.Status.VENTELISTE, now, null, null, null).getStatus() shouldBe PABEGYNT
	}


})

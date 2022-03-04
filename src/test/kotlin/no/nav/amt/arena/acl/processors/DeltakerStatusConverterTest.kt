package no.nav.amt.arena.acl.processors

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.*
import no.nav.amt.arena.acl.processors.converters.DeltakerStatusConverter
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

	val converter = DeltakerStatusConverter(SimpleMeterRegistry())

	"status - AKTUELL - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("AKTUELL", now, null, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - AKTUELL og har startdato i fortid - returnerer DELTAR" {
		converter.convert("AKTUELL", now, yesterday, null, null) shouldBe DELTAR
	}
	"status - AKTUELL og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("AKTUELL", now, tomorrow, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - AKTUELL og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		converter.convert("AKTUELL", now, yesterday.minusDays(1), yesterday, null) shouldBe HAR_SLUTTET
	}
	"status - AKTUELL og har sluttdato i fremtid - returnerer DELTAR" {
		converter.convert("AKTUELL", now, yesterday, tomorrow, null) shouldBe DELTAR
	}
	"status - AKTUELL og har startdato i dag - returnerer DELTAR" {
		converter.convert("AKTUELL", now, now.toLocalDate(), tomorrow, null) shouldBe DELTAR
	}
	"status - AVSLAG og mangler startdato - returnerer IKKE_AKTUELL" {
		converter.convert("AVSLAG", now, null, null, null) shouldBe IKKE_AKTUELL
	}


	"status - DELAVB og mangler startdato - returnerer IKKE_AKTUELL" {
		converter.convert("DELAVB", now, null, null, null) shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		converter.convert("DELAVB", now, yesterday.minusDays(1), null, yesterday) shouldBe HAR_SLUTTET
	}
	"status - DELAVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		converter.convert("DELAVB", now, yesterday, null, yesterday.minusDays(1)) shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		converter.convert("DELAVB", now, tomorrow, null, null) shouldBe IKKE_AKTUELL
	}

	"status - FULLF og mangler startdato - returnerer IKKE_AKTUELL" {
		converter.convert("FULLF", now, null, null, null) shouldBe IKKE_AKTUELL
	}
	"status - FULLF og har startdato før endretdato - returnerer HAR_SLUTTET" {
		converter.convert("FULLF", now, yesterday.minusDays(1), null, yesterday) shouldBe HAR_SLUTTET
	}
	"status - FULLF og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		converter.convert("FULLF", now, yesterday, null, yesterday.minusDays(1)) shouldBe IKKE_AKTUELL
	}
	"status - FULLF og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		converter.convert("FULLF", now, tomorrow, null, null) shouldBe IKKE_AKTUELL
	}

	"status - GJENN - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("GJENN", now, null, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - GJENN og har startdato i fortid - returnerer GJENNOMFORES" {
		converter.convert("GJENN", now, yesterday, null, null) shouldBe DELTAR
	}
	"status - GJENN og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("GJENN", now, tomorrow, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - GJENN og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		converter.convert("GJENN", now, yesterday.minusDays(1), yesterday, null) shouldBe HAR_SLUTTET
	}
	"status - GJENN og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		converter.convert("GJENN", now, yesterday, tomorrow, null) shouldBe DELTAR
	}


	"status - GJENN_AVB og mangler startdato - returnerer IKKE_AKTUELL" {
		converter.convert("GJENN_AVB", now, null, null, null) shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		converter.convert("GJENN_AVB", now, yesterday.minusDays(1), null, yesterday) shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		converter.convert("GJENN_AVB", now, yesterday, null, yesterday.minusDays(1)) shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		converter.convert("GJENN_AVB", now, tomorrow, null, null) shouldBe IKKE_AKTUELL
	}


	"status - GJENN_AVL og mangler startdato - returnerer IKKE_AKTUELL" {
		converter.convert("GJENN_AVL", now, null, null, null) shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og har startdato før endretdato - returnerer HAR_SLUTTET" {
		converter.convert("GJENN_AVL", now, yesterday.minusDays(1), null, yesterday) shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVL og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		converter.convert("GJENN_AVL", now, yesterday, null, yesterday.minusDays(1)) shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		converter.convert("GJENN_AVL", now, tomorrow, null, null) shouldBe IKKE_AKTUELL
	}


	"status - IKKAKTUELL - returnerer IKKE_AKTUELL" {
		converter.convert("IKKAKTUELL", now, null, null, null) shouldBe IKKE_AKTUELL
	}

	"status - IKKAKTUELL - returnerer FEILREGISTRERT hvis registrert dato er samme dag som status endring" {
		converter.convert("IKKAKTUELL", now, null, null, now.toLocalDate()) shouldBe FEILREGISTRERT
	}


	"status - IKKEM og mangler startdato - returnerer IKKE_AKTUELL" {
		converter.convert("IKKEM", now, null, null, null) shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato før endretdato - returnerer HAR_SLUTTET" {
		converter.convert("IKKEM", now, yesterday.minusDays(1), null, yesterday) shouldBe HAR_SLUTTET
	}
	"status - IKKEM og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		converter.convert("IKKEM", now, yesterday, null, yesterday.minusDays(1)) shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		converter.convert("IKKEM", now, tomorrow, null, null) shouldBe IKKE_AKTUELL
	}


	"status - INFOMOETE - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("INFOMOETE", now, null, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - INFOMOETE og har startdato i fortid - returnerer GJENNOMFORES" {
		converter.convert("INFOMOETE", now, yesterday, null, null) shouldBe DELTAR
	}
	"status - INFOMOETE og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("INFOMOETE", now, tomorrow, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - INFOMOETE og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		converter.convert("INFOMOETE", now, yesterday.minusDays(1), yesterday, null) shouldBe HAR_SLUTTET
	}
	"status - INFOMOETE og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		converter.convert("INFOMOETE", now, yesterday, tomorrow, null) shouldBe DELTAR
	}

	"status - JATAKK - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("JATAKK", now, null, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - JATAKK og har startdato i fortid - returnerer GJENNOMFORES" {
		converter.convert("JATAKK", now, yesterday, null, null) shouldBe DELTAR
	}
	"status - JATAKK og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("JATAKK", now, tomorrow, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - JATAKK og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		converter.convert("JATAKK", now, yesterday.minusDays(1), yesterday, null) shouldBe HAR_SLUTTET
	}
	"status - JATAKK og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		converter.convert("JATAKK", now, yesterday, tomorrow, null) shouldBe DELTAR
	}

	"status - NEITAKK - returnerer IKKE_AKTUELL" {
		converter.convert("NEITAKK", now, null, null, null) shouldBe IKKE_AKTUELL
	}


	"status - TILBUD - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("TILBUD", now, null, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - TILBUD og har startdato i fortid - returnerer GJENNOMFORES" {
		converter.convert("TILBUD", now, yesterday, null, null) shouldBe DELTAR
	}
	"status - TILBUD og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("TILBUD", now, tomorrow, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - TILBUD og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		converter.convert("TILBUD", now, yesterday.minusDays(1), yesterday, null) shouldBe HAR_SLUTTET
	}
	"status - TILBUD og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		converter.convert("TILBUD", now, yesterday, tomorrow, null) shouldBe DELTAR
	}


	"status - VENTELISTE - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("VENTELISTE", now, null, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - VENTELISTE og har startdato i fortid - returnerer GJENNOMFORES" {
		converter.convert("VENTELISTE", now, yesterday, null, null) shouldBe DELTAR
	}
	"status - VENTELISTE og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		converter.convert("VENTELISTE", now, tomorrow, null, null) shouldBe VENTER_PA_OPPSTART
	}
	"status - VENTELISTE og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		converter.convert("VENTELISTE", now, yesterday.minusDays(1), yesterday, null) shouldBe HAR_SLUTTET
	}
	"status - VENTELISTE og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		converter.convert("VENTELISTE", now, yesterday, tomorrow, null) shouldBe DELTAR
	}

})

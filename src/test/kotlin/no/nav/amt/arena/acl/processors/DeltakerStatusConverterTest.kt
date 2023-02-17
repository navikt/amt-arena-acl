package no.nav.amt.arena.acl.processors

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.*
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.processors.converters.ArenaDeltakerStatusConverter.convert
import no.nav.amt.arena.acl.processors.converters.GjennomforingStatus
import java.time.LocalDate
import java.time.LocalDateTime

private val tomorrow = LocalDate.now().plusDays(1)
private val yesterday = LocalDate.now().minusDays(1)
private val now = LocalDateTime.now()

class DeltakerStatusConverterTest : StringSpec({
	val gjennomforingStatus = GjennomforingStatus.GJENNOMFORES

	"status - AKTUELL - returnerer PABEGYNT" {
		val status = convert(TiltakDeltaker.Status.AKTUELL, now, null, null, null, gjennomforingStatus)

		status.navn shouldBe PABEGYNT
	}

	"status - AVSLAG og mangler startdato - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.AVSLAG,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}

	"status - DELAVB og mangler startdato - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.DELAVB,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		convert(TiltakDeltaker.Status.DELAVB, now, yesterday.minusDays(1), null, yesterday.atStartOfDay(), gjennomforingStatus).navn shouldBe HAR_SLUTTET
		convert(
			TiltakDeltaker.Status.DELAVB,
			now,
			yesterday.minusDays(1),
			null,
			yesterday.atStartOfDay(),
			gjennomforingStatus
		).navn shouldBe HAR_SLUTTET
	}
	"status - DELAVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		convert(TiltakDeltaker.Status.DELAVB, now, yesterday, null, yesterday.minusDays(1).atStartOfDay(), gjennomforingStatus).navn shouldBe IKKE_AKTUELL
		convert(
			TiltakDeltaker.Status.DELAVB,
			now,
			yesterday,
			null,
			yesterday.minusDays(1).atStartOfDay(),
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.DELAVB,
			now,
			tomorrow,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}

	"status - FULLF og mangler startdato - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.FULLF,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}
	"status - FULLF og har startdato før endretdato - returnerer HAR_SLUTTET" {
		val endretDato = yesterday.atTime(13, 30)
		val status = convert(TiltakDeltaker.Status.FULLF, now, endretDato.minusDays(1).toLocalDate(), null, endretDato, gjennomforingStatus)
		status.navn shouldBe HAR_SLUTTET
		status.endretDato shouldBe endretDato
	}
	"status - FULLF og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		val endretDato = yesterday.minusDays(1).atTime(13, 30)

		val status = convert(TiltakDeltaker.Status.FULLF, now, yesterday, null, endretDato, gjennomforingStatus)

		status.navn shouldBe IKKE_AKTUELL
		status.endretDato shouldBe endretDato
	}
	"status - FULLF og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.FULLF,
			now,
			tomorrow,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}

	"status - FULLF og har endretdato etter sluttdato - returnerer HAR_SLUTTET" {
		val sluttDato = now.toLocalDate().atTime(12, 30)
		val status = convert(TiltakDeltaker.Status.FULLF, now, sluttDato.minusDays(1).toLocalDate(), sluttDato.toLocalDate(), sluttDato, gjennomforingStatus)
		status.navn shouldBe HAR_SLUTTET
		status.endretDato shouldBe sluttDato
	}

	"status - GJENN - returnerer VENTER_PÅ_OPPSTART" {
		convert(
			TiltakDeltaker.Status.GJENN,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe VENTER_PA_OPPSTART
	}
	"status - GJENN og har startdato i fortid - returnerer GJENNOMFORES" {
		val startDato = yesterday.minusDays(1)
		val status = convert(TiltakDeltaker.Status.GJENN, now, startDato, null, startDato.plusDays(1).atTime(12, 30), gjennomforingStatus)
		status.navn shouldBe DELTAR
		status.endretDato shouldBe startDato.atStartOfDay()
	}
	"status - GJENN og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		val datoEndret = tomorrow.atTime(10,30)
		val status = convert(TiltakDeltaker.Status.GJENN, now, datoEndret.plusDays(2).toLocalDate(), null, datoEndret, gjennomforingStatus)
		status.navn shouldBe VENTER_PA_OPPSTART
		status.endretDato shouldBe datoEndret
	}
	"status - GJENN og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		convert(
			TiltakDeltaker.Status.GJENN,
			now,
			yesterday.minusDays(1),
			yesterday,
			null,
			gjennomforingStatus
		).navn shouldBe HAR_SLUTTET
	}
	"status - GJENN og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		val startDato = yesterday
		val status = convert(TiltakDeltaker.Status.GJENN, now, startDato, tomorrow.plusDays(1), tomorrow.atTime(10, 20), gjennomforingStatus)
		status.navn shouldBe DELTAR
		status.endretDato shouldBe startDato.atStartOfDay()
	}
	"status - GJENN og har sluttdato har passert - returnerer HAR_SLUTTET" {
		val sluttDato = yesterday
		val status = convert(
			TiltakDeltaker.Status.GJENN,
			now,
			sluttDato.minusDays(1),
			sluttDato,
			sluttDato.plusDays(1).atTime(10, 20),
			gjennomforingStatus
		)
		status.navn shouldBe HAR_SLUTTET
		status.endretDato shouldBe sluttDato.atStartOfDay()
	}


	"status - GJENN_AVB og mangler startdato - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.GJENN_AVB,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		convert(TiltakDeltaker.Status.GJENN_AVB, now, yesterday.minusDays(1), null, yesterday.atStartOfDay(), gjennomforingStatus).navn shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		convert(TiltakDeltaker.Status.GJENN_AVB, now, yesterday, null, yesterday.minusDays(1).atStartOfDay(), gjennomforingStatus).navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.GJENN_AVB,
			now,
			tomorrow,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}


	"status - GJENN_AVL og mangler startdato - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.GJENN_AVL,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og har startdato før endretdato - returnerer HAR_SLUTTET" {
		convert(TiltakDeltaker.Status.GJENN_AVL, now, yesterday.minusDays(1), null, yesterday.atStartOfDay(), gjennomforingStatus).navn shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVL og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		convert(TiltakDeltaker.Status.GJENN_AVL, now, yesterday, null, yesterday.minusDays(1).atStartOfDay(), gjennomforingStatus).navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.GJENN_AVL,
			now,
			tomorrow,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}


	"status - IKKAKTUELL - returnerer IKKE_AKTUELL" {
		val statusEndret = yesterday.minusDays(5)
		val status = convert(TiltakDeltaker.Status.IKKAKTUELL, now, statusEndret.plusDays(2), statusEndret.plusDays(4), statusEndret.atStartOfDay(), gjennomforingStatus)
		status.navn shouldBe IKKE_AKTUELL
		status.endretDato shouldBe statusEndret.atStartOfDay()
	}

	"status - IKKAKTUELL - returnerer FEILREGISTRERT hvis registrert dato er samme dag som status endring" {
		convert(TiltakDeltaker.Status.IKKAKTUELL, now, null, null, now, gjennomforingStatus).navn shouldBe FEILREGISTRERT
	}


	"status - IKKEM og mangler startdato - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.IKKEM,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato før endretdato - returnerer HAR_SLUTTET" {
		convert(TiltakDeltaker.Status.IKKEM, now, yesterday.minusDays(1), null, yesterday.atStartOfDay(), gjennomforingStatus).navn shouldBe HAR_SLUTTET
	}
	"status - IKKEM og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		convert(TiltakDeltaker.Status.IKKEM, now, yesterday, null, yesterday.minusDays(1).atStartOfDay(), gjennomforingStatus).navn shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.IKKEM,
			now,
			tomorrow,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}
	"status - INFOMOETE - returnerer PABEGYNT" {
		convert(
			TiltakDeltaker.Status.INFOMOETE,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe PABEGYNT
	}
	"status - JATAKK - returnerer VENTER_PÅ_OPPSTART" {
		convert(
			TiltakDeltaker.Status.JATAKK,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe VENTER_PA_OPPSTART
	}
	"status - JATAKK og har startdato i fortid - returnerer GJENNOMFORES" {
		convert(
			TiltakDeltaker.Status.JATAKK,
			now,
			yesterday,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe DELTAR
	}
	"status - JATAKK og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		convert(
			TiltakDeltaker.Status.JATAKK,
			now,
			tomorrow,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe VENTER_PA_OPPSTART
	}
	"status - JATAKK og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		convert(
			TiltakDeltaker.Status.JATAKK,
			now,
			yesterday.minusDays(1),
			yesterday,
			null,
			gjennomforingStatus
		).navn shouldBe HAR_SLUTTET
	}
	"status - JATAKK og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		convert(
			TiltakDeltaker.Status.JATAKK,
			now,
			yesterday,
			tomorrow,
			null,
			gjennomforingStatus
		).navn shouldBe DELTAR
	}
	"status - NEITAKK - returnerer IKKE_AKTUELL" {
		convert(
			TiltakDeltaker.Status.NEITAKK,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe IKKE_AKTUELL
	}
	"status - TILBUD - returnerer VENTER_PÅ_OPPSTART" {
		convert(
			TiltakDeltaker.Status.TILBUD,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe VENTER_PA_OPPSTART
	}
	"status - TILBUD og har startdato i fortid - returnerer GJENNOMFORES" {
		convert(
			TiltakDeltaker.Status.TILBUD,
			now,
			yesterday,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe DELTAR
	}
	"status - TILBUD og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		convert(
			TiltakDeltaker.Status.TILBUD,
			now,
			tomorrow,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe VENTER_PA_OPPSTART
	}
	"status - TILBUD og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		convert(
			TiltakDeltaker.Status.TILBUD,
			now,
			yesterday.minusDays(1),
			yesterday,
			null,
			gjennomforingStatus
		).navn shouldBe HAR_SLUTTET
	}
	"status - TILBUD og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		convert(
			TiltakDeltaker.Status.TILBUD,
			now,
			yesterday,
			tomorrow,
			null,
			gjennomforingStatus
		).navn shouldBe DELTAR
	}
	"status - PABEGYNT - returnerer VENTER_PÅ_OPPSTART" {
		convert(
			TiltakDeltaker.Status.VENTELISTE,
			now,
			null,
			null,
			null,
			gjennomforingStatus
		).navn shouldBe PABEGYNT
	}
})

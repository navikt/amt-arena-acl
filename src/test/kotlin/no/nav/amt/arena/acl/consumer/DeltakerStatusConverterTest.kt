package no.nav.amt.arena.acl.consumer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.consumer.converters.ArenaDeltakerStatusConverter
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.AVBRUTT
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.DELTAR
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.FEILREGISTRERT
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.FULLFORT
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.HAR_SLUTTET
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.IKKE_AKTUELL
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.SOKT_INN
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.VENTELISTE
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.VENTER_PA_OPPSTART
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker.Status.VURDERES
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import java.time.LocalDate
import java.time.LocalDateTime

class DeltakerStatusArenaDeltakerStatusConvertererTest : StringSpec({
	val erGjennomforingAvsluttet = false

	"status - AKTUELL - returnerer SOKT_INN" {
		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.AKTUELL,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert()

		status.navn shouldBe SOKT_INN
	}

	"status - FEILREG - returnerer FEILREGISTRERT" {
		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.FEILREG,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert()

		status.navn shouldBe FEILREGISTRERT
	}

	"status - AVSLAG og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.AVSLAG,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"status - DELAVB og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.DELAVB,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false,
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.DELAVB,
			now,
			yesterday.minusDays(1),
			null,
			false,
			yesterday.atStartOfDay(),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe HAR_SLUTTET
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.DELAVB,
			now,
			yesterday.minusDays(1),
			null,
			false,
			yesterday.atStartOfDay(),
			erGjennomforingAvsluttet, LocalDate.now(), false,
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - DELAVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.DELAVB,
			now,
			yesterday,
			null,
			false,
			yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe IKKE_AKTUELL
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.DELAVB,
			now,
			yesterday,
			null,
			false,
			yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.DELAVB,
			now,
			tomorrow,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false,
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"status - FULLF og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.FULLF,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false,
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - FULLF og har startdato før endretdato - returnerer HAR_SLUTTET" {
		val endretDato = yesterday.atTime(13, 30)
		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.FULLF,
			now,
			endretDato.minusDays(1).toLocalDate(),
			null,
			false,
			endretDato,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert()
		status.navn shouldBe HAR_SLUTTET
		status.endretDato shouldBe endretDato
	}
	"status - FULLF og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		val endretDato = yesterday.minusDays(1).atTime(13, 30)

		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.FULLF,
			now,
			yesterday,
			null,
			false,
			endretDato,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert()

		status.navn shouldBe IKKE_AKTUELL
		status.endretDato shouldBe endretDato
	}
	"status - FULLF og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.FULLF,
			now,
			tomorrow,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"status - FULLF og har endretdato etter sluttdato - returnerer HAR_SLUTTET" {
		val sluttDato = now.toLocalDate().atTime(12, 30)
		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.FULLF,
			now,
			deltakerStartdato = sluttDato.minusDays(1).toLocalDate(),
			deltakerSluttdato = sluttDato.toLocalDate(),
			false,
			datoStatusEndring = sluttDato,
			erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			false
		).convert()
		status.navn shouldBe HAR_SLUTTET
	}

	"status - GJENN - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}
	"status - GJENN og har startdato i fortid - returnerer GJENNOMFORES" {
		val startDato = yesterday.minusDays(1)
		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			startDato,
			null,
			false,
			startDato.plusDays(1).atTime(12, 30),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert()
		status.navn shouldBe DELTAR
		status.endretDato shouldBe startDato.atStartOfDay()
	}
	"status - GJENN og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		val datoEndret = tomorrow.atTime(10, 30)
		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			datoEndret.plusDays(2).toLocalDate(),
			null,
			false,
			datoEndret,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert()
		status.navn shouldBe VENTER_PA_OPPSTART
		status.endretDato shouldBe datoEndret
	}
	"status - GJENN og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			yesterday.minusDays(1),
			yesterday,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - GJENN og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		val startDato = yesterday
		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			startDato,
			tomorrow.plusDays(1),
			false,
			tomorrow.atTime(10, 20),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert()
		status.navn shouldBe DELTAR
		status.endretDato shouldBe startDato.atStartOfDay()
	}
	"status - GJENN og har sluttdato har passert - returnerer HAR_SLUTTET" {
		val sluttDato = yesterday
		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			sluttDato.minusDays(1),
			sluttDato,
			false,
			sluttDato.plusDays(1).atTime(10, 20),
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert()
		status.navn shouldBe HAR_SLUTTET
		status.endretDato shouldBe sluttDato.atStartOfDay()
	}


	"status - GJENN_AVB og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN_AVB,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN_AVB,
			now,
			yesterday.minusDays(1),
			null,
			false,
			yesterday.atStartOfDay(),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN_AVB,
			now,
			yesterday,
			null,
			false,
			yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN_AVB,
			now,
			tomorrow,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN_AVL,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN_AVL,
			now,
			yesterday.minusDays(1),
			null,
			false,
			yesterday.atStartOfDay(),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVL og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN_AVL,
			now,
			yesterday,
			null,
			false,
			yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN_AVL,
			now,
			tomorrow,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"status - IKKAKTUELL - returnerer IKKE_AKTUELL" {
		val statusEndret = yesterday.minusDays(5)
		val status = ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.IKKAKTUELL,
			now,
			statusEndret.plusDays(2),
			statusEndret.plusDays(4),
			false,
			statusEndret.atStartOfDay(),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert()
		status.navn shouldBe IKKE_AKTUELL
		status.endretDato shouldBe statusEndret.atStartOfDay()
	}

	"status - IKKAKTUELL - returnerer FEILREGISTRERT hvis registrert dato er samme dag som status endring" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.IKKAKTUELL,
			now,
			null,
			null,
			false,
			now,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe FEILREGISTRERT
	}

	"status - IKKEM og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.IKKEM,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.IKKEM,
			now,
			yesterday.minusDays(1),
			null,
			false,
			yesterday.atStartOfDay(),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - IKKEM og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.IKKEM,
			now,
			yesterday,
			null,
			false,
			yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.IKKEM,
			now,
			tomorrow,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - INFOMOETE - returnerer VURDERES" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.INFOMOETE,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe VURDERES
	}
	"status - JATAKK - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.JATAKK,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}
	"status - JATAKK og har startdato i fortid - returnerer GJENNOMFORES" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.JATAKK,
			now,
			yesterday,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe DELTAR
	}
	"status - JATAKK og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.JATAKK,
			now,
			tomorrow,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}
	"status - JATAKK og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.JATAKK,
			now,
			yesterday.minusDays(1),
			yesterday,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - JATAKK og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.JATAKK,
			now,
			yesterday,
			tomorrow,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe DELTAR
	}
	"status - NEITAKK - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.NEITAKK,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - TILBUD - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.TILBUD,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}
	"status - TILBUD og har startdato i fortid - returnerer GJENNOMFORES" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.TILBUD,
			now,
			yesterday,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe DELTAR
	}
	"status - TILBUD og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.TILBUD,
			now,
			tomorrow,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}
	"status - TILBUD og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.TILBUD,
			now,
			yesterday.minusDays(1),
			yesterday,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - TILBUD og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.TILBUD,
			now,
			yesterday,
			tomorrow,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe DELTAR
	}
	"status - VENTELISTE - returnerer VENTELISTE" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.VENTELISTE,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet, LocalDate.now(), false
		).convert().navn shouldBe VENTELISTE
	}

	"convert - VENTELISTE på kurstiltak - returnerer VENTELISTE" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.VENTELISTE,
			now,
			LocalDate.now(),
			null,
			false,
			LocalDateTime.now(),
			erGjennomforingAvsluttet, LocalDate.now(), true
		).convert().navn shouldBe VENTELISTE
	}

	"convert - AKTUELL på kurstiltak - returnerer SOKT_INN" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.AKTUELL,
			now,
			LocalDate.now(),
			null,
			false,
			LocalDateTime.now(),
			erGjennomforingAvsluttet, LocalDate.now(), true
		).convert().navn shouldBe SOKT_INN
	}

	"convert - GODKJENT_TILTAKSPLASS på kurstiltak - returnerer AVBRUTT om man ikke har gått på kurset" {
		// kurs start -> deltaker start -> deltaker slutt -> kurs slutt
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.TILBUD,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now().minusDays(4),
			deltakerSluttdato = LocalDate.now().minusDays(3),
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now().minusDays(5),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			deltakelseKreverGodkjenningLoep = true
		).convert().navn shouldBe AVBRUTT
	}

	"convert - GODKJENT_TILTAKSPLASS på kurstiltak - returnerer FULLFORT om man er ferdig samme dag som kurset" {
		// kurs start -> deltaker start -> deltaker slutt = kurs slutt
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.TILBUD,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now().minusDays(4),
			deltakerSluttdato = LocalDate.now().minusDays(3),
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now().minusDays(5),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now().minusDays(3),
			deltakelseKreverGodkjenningLoep = true
		).convert().navn shouldBe FULLFORT
	}

	"convert - GODKJENT_TILTAKSPLASS på kurstiltak - VENTER_PA_OPPSTART" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.TILBUD,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now().plusDays(1),
			deltakerSluttdato = LocalDate.now().plusDays(3),
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now().plusDays(3),
			deltakelseKreverGodkjenningLoep = true
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}

	"convert - FULLF samme dag som kurset er ferdig - blir FULLFORT" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.FULLF,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now().plusDays(1),
			deltakerSluttdato = LocalDate.now().plusDays(3),
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now().plusDays(3),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now().plusDays(3),
			deltakelseKreverGodkjenningLoep = true
		).convert().navn shouldBe FULLFORT
	}

	"convert - FULLF før kurset er ferdig - blir FULLFORT" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.FULLF,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now().plusDays(1),
			deltakerSluttdato = LocalDate.now().plusDays(2),
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now().plusDays(2),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now().plusDays(3),
			deltakelseKreverGodkjenningLoep = true
		).convert().navn shouldBe FULLFORT
	}

	"convert - DELAVB før kurset er ferdig - blir AVBRUTT" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.DELAVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now().plusDays(1),
			deltakerSluttdato = LocalDate.now().plusDays(2),
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now().plusDays(2),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now().plusDays(3),
			deltakelseKreverGodkjenningLoep = true
		).convert().navn shouldBe AVBRUTT
	}

	"convert - DELAVB etter kurset er ferdig - blir AVBRUTT" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.DELAVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now().plusDays(1),
			deltakerSluttdato = LocalDate.now().plusDays(4),
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now().plusDays(2),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now().plusDays(3),
			deltakelseKreverGodkjenningLoep = true
		).convert().navn shouldBe AVBRUTT
	}

	"convert - FULLF kurs varer en dag - blir FULLFORT" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.FULLF,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now().plusDays(1),
			deltakerSluttdato = LocalDate.now().plusDays(1),
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now().plusDays(1),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now().plusDays(1),
			deltakelseKreverGodkjenningLoep = true
		).convert().navn shouldBe FULLFORT
	}
}) {
	companion object {
		private val tomorrow = LocalDate.now().plusDays(1)
		private val yesterday = LocalDate.now().minusDays(1)
		private val now = LocalDateTime.now()
	}
}

class EnkeltplassStatusConverterTest : StringSpec({
	val erGjennomforingAvsluttet = true

	// Verifiser at med erEnkeltplass=false blir en ikke-avsluttende status overstyrt til IKKE_AKTUELL
	// når gjennomføringen er avsluttet
	"erEnkeltplass=false og gjennomforing avsluttet - GJENN uten startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"erEnkeltplass=false og gjennomforing avsluttet - GJENN med startdato i fortid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			yesterday,
			null,
			false,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"erEnkeltplass=false og gjennomforing avsluttet - AKTUELL - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.AKTUELL,
			now,
			null,
			null,
			false,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	// Med erEnkeltplass=true skal ikke avsluttet gjennomføring overstyre statusen
	"erEnkeltplass=true og gjennomforing avsluttet - GJENN uten startdato - returnerer VENTER_PA_OPPSTART" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			null,
			null,
			true,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}

	"erEnkeltplass=true og gjennomforing avsluttet - GJENN med startdato i fortid - returnerer DELTAR" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			yesterday,
			null,
			true,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe DELTAR
	}

	"erEnkeltplass=true og gjennomforing avsluttet - JATAKK uten startdato - returnerer VENTER_PA_OPPSTART" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.JATAKK,
			now,
			null,
			null,
			true,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}

	"erEnkeltplass=true og gjennomforing avsluttet - TILBUD uten startdato - returnerer VENTER_PA_OPPSTART" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.TILBUD,
			now,
			null,
			null,
			true,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}

	"erEnkeltplass=true og gjennomforing avsluttet - AKTUELL - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.AKTUELL,
			now,
			null,
			null,
			true,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			true
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"erEnkeltplass=true og gjennomforing avsluttet - INFOMOETE - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.INFOMOETE,
			now,
			null,
			null,
			true,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"erEnkeltplass=true og gjennomforing avsluttet - GJENN - returnerer Deltar" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			LocalDate.now().minusDays(2),
			LocalDate.now().plusDays(2),
			true,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe DELTAR
	}

	"erEnkeltplass=true og gjennomforing avsluttet - VENTELISTE - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.VENTELISTE,
			now,
			null,
			null,
			true,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	// Avsluttende statuser skal passere gjennom uavhengig av erEnkeltplass
	"erEnkeltplass=true og gjennomforing avsluttet - GJENN med passert sluttdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			yesterday.minusDays(1),
			yesterday,
			true,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe HAR_SLUTTET
	}

	"erEnkeltplass=false og gjennomforing avsluttet - GJENN med passert sluttdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			TiltakDeltaker.Status.GJENN,
			now,
			yesterday.minusDays(1),
			yesterday,
			false,
			null,
			erGjennomforingAvsluttet,
			LocalDate.now(),
			false
		).convert().navn shouldBe HAR_SLUTTET
	}
}) {
	companion object {
		private val yesterday = LocalDate.now().minusDays(1)
		private val now = LocalDateTime.now()
	}
}


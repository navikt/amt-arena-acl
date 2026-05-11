package no.nav.amt.arena.acl.consumer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.clients.mulighetsrommet.Gjennomforing
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
			arenaStatus = TiltakDeltaker.Status.AKTUELL,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()

		status.navn shouldBe SOKT_INN
	}

	"status - FEILREG - returnerer FEILREGISTRERT" {
		val status = ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.FEILREG,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()

		status.navn shouldBe FEILREGISTRERT
	}

	"status - AVSLAG og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.AVSLAG,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
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
			erGjennomforingAvsluttet, LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			false,
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.DELAVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday.minusDays(1),
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = yesterday.atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe HAR_SLUTTET
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.DELAVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday.minusDays(1),
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = yesterday.atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false,
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - DELAVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.DELAVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.DELAVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - DELAVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.DELAVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = tomorrow,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false,
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"status - FULLF og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.FULLF,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false,
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - FULLF og har startdato før endretdato - returnerer HAR_SLUTTET" {
		val endretDato = yesterday.atTime(13, 30)
		val status = ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.FULLF,
			deltakerRegistrertDato = now,
			deltakerStartdato = endretDato.minusDays(1).toLocalDate(),
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = endretDato,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()
		status.navn shouldBe HAR_SLUTTET
		status.endretDato shouldBe endretDato
	}
	"status - FULLF og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		val endretDato = yesterday.minusDays(1).atTime(13, 30)

		val status = ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.FULLF,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = endretDato,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()

		status.navn shouldBe IKKE_AKTUELL
		status.endretDato shouldBe endretDato
	}
	"status - FULLF og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.FULLF,
			deltakerRegistrertDato = now,
			deltakerStartdato = tomorrow,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"status - FULLF og har endretdato etter sluttdato - returnerer HAR_SLUTTET" {
		val sluttDato = now.toLocalDate().atTime(12, 30)
		val status = ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.FULLF,
			deltakerRegistrertDato = now,
			deltakerStartdato = sluttDato.minusDays(1).toLocalDate(),
			deltakerSluttdato = sluttDato.toLocalDate(),
			erEnkeltplass = false,
			datoStatusEndring = sluttDato,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()
		status.navn shouldBe HAR_SLUTTET
	}

	"status - GJENN - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}
	"status - GJENN og har startdato i fortid - returnerer GJENNOMFORES" {
		val startDato = yesterday.minusDays(1)
		val status = ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = startDato,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = startDato.plusDays(1).atTime(12, 30),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()
		status.navn shouldBe DELTAR
		status.endretDato shouldBe startDato.atStartOfDay()
	}
	"status - GJENN og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		val datoEndret = tomorrow.atTime(10, 30)
		val status = ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = datoEndret.plusDays(2).toLocalDate(),
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = datoEndret,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()
		status.navn shouldBe VENTER_PA_OPPSTART
		status.endretDato shouldBe datoEndret
	}
	"status - GJENN og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday.minusDays(1),
			deltakerSluttdato = yesterday,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - GJENN og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		val startDato = yesterday
		val status = ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = startDato,
			deltakerSluttdato = tomorrow.plusDays(1),
			erEnkeltplass = false,
			datoStatusEndring = tomorrow.atTime(10, 20),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()
		status.navn shouldBe DELTAR
		status.endretDato shouldBe startDato.atStartOfDay()
	}
	"status - GJENN og har sluttdato har passert - returnerer HAR_SLUTTET" {
		val sluttDato = yesterday
		val status = ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = sluttDato.minusDays(1),
			deltakerSluttdato = sluttDato,
			erEnkeltplass = false,
			datoStatusEndring = sluttDato.plusDays(1).atTime(10, 20),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()
		status.navn shouldBe HAR_SLUTTET
		status.endretDato shouldBe sluttDato.atStartOfDay()
	}


	"status - GJENN_AVB og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN_AVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN_AVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday.minusDays(1),
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = yesterday.atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVB og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN_AVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVB og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN_AVB,
			deltakerRegistrertDato = now,
			deltakerStartdato = tomorrow,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN_AVL,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
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
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - GJENN_AVL og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN_AVL,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - GJENN_AVL og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN_AVL,
			deltakerRegistrertDato = now,
			deltakerStartdato = tomorrow,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"status - IKKAKTUELL - returnerer IKKE_AKTUELL" {
		val statusEndret = yesterday.minusDays(5)
		val status = ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.IKKAKTUELL,
			deltakerRegistrertDato = now,
			deltakerStartdato = statusEndret.plusDays(2),
			deltakerSluttdato = statusEndret.plusDays(4),
			erEnkeltplass = false,
			datoStatusEndring = statusEndret.atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert()
		status.navn shouldBe IKKE_AKTUELL
		status.endretDato shouldBe statusEndret.atStartOfDay()
	}

	"status - IKKAKTUELL - returnerer FEILREGISTRERT hvis registrert dato er samme dag som status endring" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.IKKAKTUELL,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = now,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe FEILREGISTRERT
	}

	"status - IKKEM og mangler startdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.IKKEM,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato før endretdato - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.IKKEM,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday.minusDays(1),
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = yesterday.atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - IKKEM og har startdato etter endretdato - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.IKKEM,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = yesterday.minusDays(1).atStartOfDay(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - IKKEM og har startdato i fremtid - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.IKKEM,
			deltakerRegistrertDato = now,
			deltakerStartdato = tomorrow,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}
	"status - INFOMOETE - returnerer VURDERES" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.INFOMOETE,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe VURDERES
	}
	"status - JATAKK - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.JATAKK,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES, deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}
	"status - JATAKK og har startdato i fortid - returnerer GJENNOMFORES" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.JATAKK,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe DELTAR
	}
	"status - JATAKK og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.JATAKK,
			deltakerRegistrertDato = now,
			deltakerStartdato = tomorrow,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false,

			).convert().navn shouldBe VENTER_PA_OPPSTART
	}
	"status - JATAKK og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.JATAKK,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday.minusDays(1),
			deltakerSluttdato = yesterday,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - JATAKK og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.JATAKK,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = tomorrow,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe DELTAR
	}
	"status - NEITAKK - returnerer IKKE_AKTUELL" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.NEITAKK,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
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
			erGjennomforingAvsluttet, LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			false
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
			erGjennomforingAvsluttet, LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			false
		).convert().navn shouldBe DELTAR
	}
	"status - TILBUD og har startdato i fremtid - returnerer VENTER_PÅ_OPPSTART" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.TILBUD,
			deltakerRegistrertDato = now,
			deltakerStartdato = tomorrow,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe VENTER_PA_OPPSTART
	}
	"status - TILBUD og har sluttdato i fortid - returnerer HAR_SLUTTET" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.TILBUD,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday.minusDays(1),
			deltakerSluttdato = yesterday,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe HAR_SLUTTET
	}
	"status - TILBUD og har sluttdato i fremtid - returnerer GJENNOMFORES" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.TILBUD,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = tomorrow,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe DELTAR
	}
	"status - VENTELISTE - returnerer VENTELISTE" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.VENTELISTE,
			deltakerRegistrertDato = now,
			deltakerStartdato = null,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe VENTELISTE
	}

	"convert - VENTELISTE på kurstiltak - returnerer VENTELISTE" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.VENTELISTE,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now(),
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
			deltakelseKreverGodkjenningLoep = true
		).convert().navn shouldBe VENTELISTE
	}

	"convert - AKTUELL på kurstiltak - returnerer SOKT_INN" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.AKTUELL,
			deltakerRegistrertDato = now,
			deltakerStartdato = LocalDate.now(),
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = LocalDateTime.now(),
			erGjennomforingAvsluttet = erGjennomforingAvsluttet, gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
			deltakelseKreverGodkjenningLoep = true
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.GJENNOMFORES,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
			false
		).convert().navn shouldBe DELTAR
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
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
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
			false
		).convert().navn shouldBe HAR_SLUTTET
	}

	// Tester som verifiserer at det er KUN kombinasjonen
	// erEnkeltplass=true + gjennomforingStatus=AVSLUTTET + arenaStatus=GJENN
	// som lar deltakelsen bli DELTAR selv om gjennomføringen har en avsluttende status.
	// Endrer man én av de tre betingelsene skal deltakelsen overstyres til IKKE_AKTUELL.

	"kun GJENN bypasser avsluttet gjennomforing - erEnkeltplass=true + AVSLUTTET + GJENN + startdato i fortid - returnerer DELTAR" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = true,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe DELTAR
	}

	"erEnkeltplass=false + AVSLUTTET + GJENN + startdato i fortid - returnerer IKKE_AKTUELL (mangler enkeltplass)" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = false,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"erEnkeltplass=true + AVBRUTT + GJENN + startdato i fortid - returnerer IKKE_AKTUELL (feil gjennomforingStatus)" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = true,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.AVBRUTT,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"erEnkeltplass=true + AVLYST + GJENN + startdato i fortid - returnerer IKKE_AKTUELL (feil gjennomforingStatus)" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.GJENN,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = true,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.AVLYST,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"erEnkeltplass=true + AVSLUTTET + JATAKK + startdato i fortid - returnerer IKKE_AKTUELL (feil arenaStatus)" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.JATAKK,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = true,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}

	"erEnkeltplass=true + AVSLUTTET + TILBUD + startdato i fortid - returnerer IKKE_AKTUELL (feil arenaStatus)" {
		ArenaDeltakerStatusConverter(
			arenaStatus = TiltakDeltaker.Status.TILBUD,
			deltakerRegistrertDato = now,
			deltakerStartdato = yesterday,
			deltakerSluttdato = null,
			erEnkeltplass = true,
			datoStatusEndring = null,
			erGjennomforingAvsluttet = erGjennomforingAvsluttet,
			gjennomforingSluttdato = LocalDate.now(),
			gjennomforingStatus = Gjennomforing.Status.AVSLUTTET,
			deltakelseKreverGodkjenningLoep = false
		).convert().navn shouldBe IKKE_AKTUELL
	}
}) {
	companion object {
		private val yesterday = LocalDate.now().minusDays(1)
		private val now = LocalDateTime.now()
	}
}


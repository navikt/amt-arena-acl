package no.nav.amt.arena.acl.processors

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.processors.converters.ArenaDeltakerAarsakConverter
import org.junit.jupiter.api.Test

class DeltakerStatusAarsakConverterTest {
	val gjennomforingStatus = AmtGjennomforing.Status.GJENNOMFORES

	@Test
	fun `convert() - årsak brukes når årsak kan mappes direkte`() {
		val actual = ArenaDeltakerAarsakConverter(
			TiltakDeltaker.Status.NEITAKK,
			AmtDeltaker.Status.HAR_SLUTTET,
			TiltakDeltaker.StatusAarsak.SYK,
			gjennomforingStatus
		).convert()

		actual shouldBe AmtDeltaker.StatusAarsak.SYK
	}

	@Test
	fun `convert() - ANNET brukes når årsak ikke kan mappes direkte`() {
		val actual = ArenaDeltakerAarsakConverter(
			TiltakDeltaker.Status.IKKAKTUELL,
			AmtDeltaker.Status.HAR_SLUTTET,
			TiltakDeltaker.StatusAarsak.UTV,
			gjennomforingStatus
		).convert()

		actual shouldBe AmtDeltaker.StatusAarsak.ANNET
	}

	@Test
	fun `convert() - årsak blir null når deltaker deltar`() {
		val actual = ArenaDeltakerAarsakConverter(
			TiltakDeltaker.Status.IKKAKTUELL,
			AmtDeltaker.Status.DELTAR,
			TiltakDeltaker.StatusAarsak.SYK,
			gjennomforingStatus
		).convert()

		actual shouldBe null
	}

	@Test
	fun `convert() - status mappes til IKKE_MOTT når årsak ikke kan mappes direkte`() {
		val actual = ArenaDeltakerAarsakConverter(
			TiltakDeltaker.Status.IKKEM,
			AmtDeltaker.Status.HAR_SLUTTET,
			TiltakDeltaker.StatusAarsak.HENLU,
			gjennomforingStatus
		).convert()

		actual shouldBe AmtDeltaker.StatusAarsak.IKKE_MOTT

	}

	@Test
	fun `convert() - status mappes til AVSLAG når årsak ikke kan mappes direkte`() {
		val actual = ArenaDeltakerAarsakConverter(
			TiltakDeltaker.Status.AVSLAG,
			AmtDeltaker.Status.HAR_SLUTTET,
			TiltakDeltaker.StatusAarsak.HENLU,
			gjennomforingStatus
		).convert()

		actual shouldBe AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS

	}

	@Test
	fun `convert() - status mappes ikke til AVSLAG når årsak kan mappes direkte`() {
		val actual = ArenaDeltakerAarsakConverter(
			TiltakDeltaker.Status.AVSLAG,
			AmtDeltaker.Status.HAR_SLUTTET,
			TiltakDeltaker.StatusAarsak.SYK,
			gjennomforingStatus
		).convert()

		actual shouldBe AmtDeltaker.StatusAarsak.SYK

	}

	@Test
	fun `convert() - årsak blir FIKK_IKKE_PLASS når gjennomføring er avsluttet mens deltaker hadde pågående status`() {
		val actual = ArenaDeltakerAarsakConverter(
			TiltakDeltaker.Status.INFOMOETE,
			AmtDeltaker.Status.IKKE_AKTUELL,
			TiltakDeltaker.StatusAarsak.SYK,
			AmtGjennomforing.Status.AVSLUTTET
		).convert()

		actual shouldBe AmtDeltaker.StatusAarsak.FIKK_IKKE_PLASS

	}


}

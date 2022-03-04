package no.nav.amt.arena.acl.processors

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.processors.converters.GjennomforingStatusConverter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GjennomforingStatusConverterTest {
	val converter = GjennomforingStatusConverter()

	@Test
	fun `convert() - konverterer avlyst til AVSLUTTET`() {
		converter.convert("AVLYST") shouldBe AmtGjennomforing.Status.AVSLUTTET
	}

	@Test
	fun `convert() - konverterer planlagt til IKKE_STARTET`() {
		converter.convert("PLANLAGT") shouldBe AmtGjennomforing.Status.IKKE_STARTET
	}

	@Test
	fun `convert() - konverterer planlagt til GJENNOMFORES`() {
		converter.convert("GJENNOMFOR") shouldBe AmtGjennomforing.Status.GJENNOMFORES
	}

	@Test
	fun `convert() - ukjent status - kaster exception `() {
		assertThrows<RuntimeException> { converter.convert("BLABLA") }
	}
}

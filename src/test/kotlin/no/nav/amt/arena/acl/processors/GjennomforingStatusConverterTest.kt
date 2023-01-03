package no.nav.amt.arena.acl.processors

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.processors.converters.GjennomforingStatusConverter.convert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GjennomforingStatusConverterTest {
	@Test
	fun `convert() - konverterer avlyst til AVSLUTTET`() {
		convert("AVLYST") shouldBe AmtGjennomforing.Status.AVSLUTTET
	}

	@Test
	fun `convert() - konverterer planlagt til IKKE_STARTET`() {
		convert("PLANLAGT") shouldBe AmtGjennomforing.Status.IKKE_STARTET
	}

	@Test
	fun `convert() - konverterer planlagt til GJENNOMFORES`() {
		convert("GJENNOMFOR") shouldBe AmtGjennomforing.Status.GJENNOMFORES
	}

	@Test
	fun `convert() - ukjent status - kaster exception `() {
		assertThrows<RuntimeException> { convert("BLABLA") }
	}
}

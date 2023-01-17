package no.nav.amt.arena.acl.processors

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.processors.converters.GjennomforingStatus
import no.nav.amt.arena.acl.processors.converters.GjennomforingStatusConverter.convert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GjennomforingStatusConverterTest {
	@Test
	fun `convert() - konverterer avlyst til AVSLUTTET`() {
		convert("AVLYST") shouldBe GjennomforingStatus.AVSLUTTET
	}

	@Test
	fun `convert() - konverterer planlagt til IKKE_STARTET`() {
		convert("PLANLAGT") shouldBe GjennomforingStatus.IKKE_STARTET
	}

	@Test
	fun `convert() - konverterer planlagt til GJENNOMFORES`() {
		convert("GJENNOMFOR") shouldBe GjennomforingStatus.GJENNOMFORES
	}

	@Test
	fun `convert() - ukjent status - kaster exception `() {
		assertThrows<RuntimeException> { convert("BLABLA") }
	}
}

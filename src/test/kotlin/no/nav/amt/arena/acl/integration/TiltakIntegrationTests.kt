package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.integration.utils.TiltakIntegrationTestInput
import org.junit.jupiter.api.Test
import java.util.*

class TiltakIntegrationTests : IntegrationTestBase() {

	@Test
	fun leggTilNyttTiltak() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()

		tiltak()
			.nyttTiltak(TiltakIntegrationTestInput(getPosition(), kode, navn))
			.arenaData { data, _ -> data.ingestStatus shouldBe IngestStatus.HANDLED }
			.tiltak { data, currentInput -> data.kode shouldBe currentInput.kode }
			.tiltak { data, currentInput -> data.navn shouldBe currentInput.navn }
	}

	@Test
	fun oppdaterTiltakOppdatererTiltakIDatabasen() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()
		val oppdatertNavn = UUID.randomUUID().toString()

		tiltak()
			.nyttTiltak(TiltakIntegrationTestInput(getPosition(), kode, navn))
			.arenaData { data, _ -> data.ingestStatus shouldBe IngestStatus.HANDLED }
			.oppdaterTiltak(getPosition(), oppdatertNavn)
			.arenaData { data, _ -> data.ingestStatus shouldBe IngestStatus.HANDLED }
			.tiltak { data, currentInput -> data.kode shouldBe currentInput.kode }
			.tiltak { data, currentInput -> data.navn shouldBe currentInput.navn }
	}

	@Test
	fun slettTiltakFeilerOgLeggerNotatPaArenaData() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()

		tiltak()
			.nyttTiltak(TiltakIntegrationTestInput(getPosition(), kode, navn))
			.arenaData { data, _ -> data.ingestStatus shouldBe IngestStatus.HANDLED }
			.slettTiltak(getPosition())
			.arenaData { data, _ -> data.ingestStatus shouldBe IngestStatus.FAILED }
			.arenaData { data, _ -> data.note shouldBe "Implementation of DELETE is not implemented." }
			.tiltak { data, _ -> data.kode shouldBe kode }
			.tiltak { data, _ -> data.navn shouldBe navn }
	}

}

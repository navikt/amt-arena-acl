package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.IngestStatus
import org.junit.jupiter.api.Test
import java.util.*

class TiltakIntegrationTests : IntegrationTestBase() {

	@Test
	fun leggTilNyttTiltak() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()

		val result = nyttTiltak(kode, navn)

		result.arenaData.ingestStatus shouldBe IngestStatus.HANDLED
		result.arenaData.arenaId shouldBe kode

		result.tiltak.kode shouldBe kode
		result.tiltak.navn shouldBe navn
	}

	@Test
	fun oppdaterTiltakOppdatererTiltakIDatabasen() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()

		nyttTiltak(kode, navn)

		val oppdatertNavn = UUID.randomUUID().toString()
		val updatedResult = oppdaterTiltak(kode, navn, oppdatertNavn)

		updatedResult.arenaData.ingestStatus shouldBe IngestStatus.HANDLED
		updatedResult.arenaData.arenaId shouldBe kode

		updatedResult.tiltak.kode shouldBe kode
		updatedResult.tiltak.navn shouldBe oppdatertNavn
	}

	@Test
	fun slettTiltakFeilerOgLeggerNotatPaArenaData() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()

		nyttTiltak(kode, navn)
		val result = slettTiltak(kode, navn)

		result.arenaData.ingestStatus shouldBe IngestStatus.FAILED
		result.arenaData.note shouldBe "Implementation of DELETE is not implemented."

		result.tiltak.kode shouldBe kode
		result.tiltak.navn shouldBe navn
	}

}

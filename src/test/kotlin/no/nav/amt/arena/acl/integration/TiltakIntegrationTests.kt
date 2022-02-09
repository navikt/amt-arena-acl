package no.nav.amt.arena.acl.integration

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
			.shouldHaveIngestStatus(IngestStatus.HANDLED)
			.shouldHaveKode(kode)
			.shouldHaveNavn(navn)
	}

	@Test
	fun oppdaterTiltakOppdatererTiltakIDatabasen() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()
		val oppdatertNavn = UUID.randomUUID().toString()

		tiltak()
			.nyttTiltak(TiltakIntegrationTestInput(getPosition(), kode, navn))
			.oppdaterTiltak(getPosition(), oppdatertNavn)
			.shouldHaveIngestStatus(IngestStatus.HANDLED)
			.shouldHaveKode(kode)
			.shouldHaveNavn(oppdatertNavn)
	}

	@Test
	fun slettTiltakFeilerOgLeggerNotatPaArenaData() {
		val kode = UUID.randomUUID().toString()
		val navn = UUID.randomUUID().toString()

		tiltak()
			.nyttTiltak(TiltakIntegrationTestInput(getPosition(), kode, navn))
			.slettTiltak(getPosition())
			.shouldHaveIngestStatus(IngestStatus.FAILED)
			.shouldHaveNote("Implementation of DELETE is not implemented.")
			.shouldHaveKode(kode)
			.shouldHaveNavn(navn)
	}

}

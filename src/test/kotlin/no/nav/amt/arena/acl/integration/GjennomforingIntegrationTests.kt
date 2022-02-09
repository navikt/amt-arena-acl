package no.nav.amt.arena.acl.integration

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.integration.utils.GjennomforingIntegrationTestInput
import no.nav.amt.arena.acl.integration.utils.TiltakIntegrationTestInput
import org.junit.jupiter.api.Test
import java.util.*

class GjennomforingIntegrationTests : IntegrationTestBase() {

	@Test
	fun leggTilNyGjennomforing() {
		val gjennomforingId = Random().nextLong()

		tiltak()
			.nyttTiltak(
				TiltakIntegrationTestInput(
					position = getPosition(),
				)
			)
			.shouldHaveIngestStatus(IngestStatus.HANDLED)

		gjennomforing()
			.nyGjennomforing(
				GjennomforingIntegrationTestInput(
					position = getPosition(),
					gjennomforingId = gjennomforingId
				)
			)
	}

}

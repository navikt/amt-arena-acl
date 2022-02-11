package no.nav.amt.arena.acl.integration

import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.integration.utils.GjennomforingIntegrationTestInput
import no.nav.amt.arena.acl.integration.utils.TiltakIntegrationTestInput
import org.junit.jupiter.api.Test
import java.util.*

// sudo rm -rf /var/run/docker.sock && sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock && TESTCONTAINERS_RYUK_DISABLED=true && colima start

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
			.shouldHaveIngestStatus(IngestStatus.HANDLED)
			.outputShouldHaveOperation(AmtOperation.CREATED)
			.validateOutput()
	}

}

package no.nav.amt.arena.acl.integration

import io.kotest.fp.success
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
			.arenaData { data, _ -> data.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforing()
			.nyGjennomforing(
				GjennomforingIntegrationTestInput(
					position = getPosition(),
					gjennomforingId = gjennomforingId
				)
			)
			.arenaData { data, _ -> data.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { data, _ -> data.operation shouldBe AmtOperation.CREATED }
			.result { result, _ -> result.translation!!.amtId shouldBe result.output!!.payload!!.id }
	}

	@Test
	fun tiltakKommerEtterGjennomforingBlirSendtVedNesteJobb() {
		val gjennomforingId = Random().nextLong()
		val tiltakNavn = UUID.randomUUID().toString()

		val gjennomforing = gjennomforing()
			.nyGjennomforing(
				GjennomforingIntegrationTestInput(
					position = getPosition(),
					gjennomforingId = gjennomforingId
				)
			)
			.arenaData { data, _ -> data.ingestStatus shouldBe IngestStatus.RETRY }
			.arenaData { data, _ -> data.ingestAttempts shouldBe 1 }
			.arenaData { data, _ -> data.lastAttempted shouldNotBe null }
			.shouldNotHaveTranslation()
			.shouldNotHaveOutput()

		tiltak()
			.nyttTiltak(
				TiltakIntegrationTestInput(
					position = getPosition(),
					navn = tiltakNavn
				)
			)

		processMessages()

		gjennomforing.updateResults()
			.arenaData { data, _ -> data.ingestStatus shouldBe IngestStatus.HANDLED }
			.arenaData { data, _ -> data.ingestedTimestamp shouldNotBe null }
			.output { data, _ -> data.payload!!.tiltak.navn shouldBe tiltakNavn }
	}

}

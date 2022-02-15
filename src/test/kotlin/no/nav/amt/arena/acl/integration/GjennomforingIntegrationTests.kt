package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.amt.arena.acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.amt.arena.acl.integration.commands.tiltak.NyttTiltakCommand
import org.junit.jupiter.api.Test
import java.util.*

// sudo rm -rf /var/run/docker.sock && sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock && TESTCONTAINERS_RYUK_DISABLED=true && colima start

class GjennomforingIntegrationTests : IntegrationTestBase() {

	@Test
	fun leggTilNyGjennomforing() {
		val gjennomforingInput = GjennomforingInput(
			gjennomforingId = Random().nextLong()
		)

		tiltakExecutor.execute(NyttTiltakCommand(navn = UUID.randomUUID().toString()))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.operation shouldBe AmtOperation.CREATED }
			.result { _, translation, output -> translation!!.amtId shouldBe output!!.payload!!.id }
	}

	@Test
	fun tiltakKommerEtterGjennomforingBlirSendtVedNesteJobb() {
		val tiltakNavn = UUID.randomUUID().toString()

		val command = NyGjennomforingCommand(
			GjennomforingInput(
				gjennomforingId = Random().nextLong()
			)
		)

		val firstResult = gjennomforingExecutor.execute(command)
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.arenaData { it.ingestAttempts shouldBe 1 }
			.arenaData { it.lastAttempted shouldNotBe null }
			.result { _, translation, _ -> translation shouldBe null }
			.result { _, _, output -> output shouldBe null }

		tiltakExecutor.execute(NyttTiltakCommand(navn = tiltakNavn))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		processMessages()

		gjennomforingExecutor.updateResults(firstResult.position, command)
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.arenaData { it.ingestedTimestamp shouldNotBe null }
			.output { it.payload!!.tiltak.navn shouldBe tiltakNavn }
	}
}

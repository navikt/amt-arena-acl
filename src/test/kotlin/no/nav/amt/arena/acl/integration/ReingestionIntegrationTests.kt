package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerInput
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerResult
import no.nav.amt.arena.acl.integration.commands.deltaker.NyDeltakerCommand
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingResult
import no.nav.amt.arena.acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.amt.arena.acl.integration.commands.sak.NySakCommand
import no.nav.amt.arena.acl.integration.commands.sak.SakInput
import no.nav.amt.arena.acl.integration.commands.tiltak.NyttTiltakCommand
import org.junit.jupiter.api.Test
import java.util.*

class ReingestionIntegrationTests : IntegrationTestBase() {

	/**
	 * Testen tester at man kan ha x antall deltakere på en gjennomføring som ikke
	 * er ingested enda, når gjennomføringen kommer blir alle deltakerene ingested
	 * på en kjøring, uansett batchsize
	 */
	@Test
	fun `Deltakere retry til gjennomføring batchsize`() {
		val gjennomforingId = Random().nextLong()

		val results = mutableListOf<Pair<NyDeltakerCommand, DeltakerResult>>()

		repeat(4) {
			val result = createDeltaker(gjennomforingId)
			results.add(result)

			result.second
				.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
		}

		ingestGjennomforingOgTiltak(gjennomforingId)

		processMessages(batchSize = 3)

		results.forEach { result ->
			deltakerExecutor.updateResults(result.second.position, result.first)
				.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
				.output { it.operation shouldBe AmtOperation.CREATED }
		}
	}

	private fun createDeltaker(gjennomforingId: Long): Pair<NyDeltakerCommand, DeltakerResult> {
		val command = NyDeltakerCommand(
			DeltakerInput(
				tiltakDeltakerId = Random().nextLong(),
				tiltakgjennomforingId = gjennomforingId,
				personId = Random().nextLong()
			)
		)

		val result = deltakerExecutor.execute(command)

		return Pair(command, result)
	}

	private fun ingestGjennomforingOgTiltak(gjennomforingId: Long): GjennomforingResult {
		val gjennomforingInput = GjennomforingInput(gjennomforingId = gjennomforingId)
		val sakInput = SakInput(sakId = gjennomforingInput.sakId)

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		sakExecutor.execute(NySakCommand(sakInput, gjennomforingId))

		return gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
	}


}

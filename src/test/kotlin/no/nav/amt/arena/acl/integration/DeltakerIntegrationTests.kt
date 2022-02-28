package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.domain.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerInput
import no.nav.amt.arena.acl.integration.commands.deltaker.NyDeltakerCommand
import no.nav.amt.arena.acl.integration.commands.deltaker.OppdaterDeltakerCommand
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingResult
import no.nav.amt.arena.acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.amt.arena.acl.integration.commands.tiltak.NyttTiltakCommand
import no.nav.amt.arena.acl.mocks.OrdsClientMock
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.util.*

class DeltakerIntegrationTests : IntegrationTestBase() {

	@Test
	fun leggTilNyDeltaker() {
		val gjennomforingId = Random().nextLong()
		val deltakerId = Random().nextLong()

		val gjennomforingResult = setupTiltakOgGjennomforing(gjennomforingId)

		val deltakerInput = DeltakerInput(tiltakDeltakerId = deltakerId, tiltakgjennomforingId = gjennomforingId)

		deltakerExecutor.execute(NyDeltakerCommand(deltakerInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.operation shouldBe AmtOperation.CREATED }
			.result { _, translation, output -> translation!!.amtId shouldBe output!!.payload!!.id }
			.outgoingPayload { it.gjennomforingId shouldBe gjennomforingResult.output!!.payload!!.id }
			.outgoingPayload { it.status shouldBe AmtDeltaker.Status.DELTAR }
	}

	@Test
	fun oppdaterDeltaker() {
		val gjennomforingId = Random().nextLong()
		val deltakerId = Random().nextLong()

		setupTiltakOgGjennomforing(gjennomforingId)

		val initialDeltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			datoTil = LocalDate.now().plusDays(1)
		)

		deltakerExecutor.execute(NyDeltakerCommand(initialDeltakerInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.outgoingPayload { it.sluttDato shouldBe initialDeltakerInput.datoTil }
			.outgoingPayload { it.status shouldBe AmtDeltaker.Status.DELTAR }

		val updatedDeltakerInput = initialDeltakerInput.copy(
			datoTil = LocalDate.now().minusDays(1)
		)

		deltakerExecutor.execute(OppdaterDeltakerCommand(initialDeltakerInput, updatedDeltakerInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.outgoingPayload { it.sluttDato shouldBe updatedDeltakerInput.datoTil }
			.outgoingPayload { it.status shouldBe AmtDeltaker.Status.HAR_SLUTTET }
	}

	@Test
	fun sameDeltakerTwiceSendsOneMessage() {
		val gjennomforingId = Random().nextLong()
		setupTiltakOgGjennomforing(gjennomforingId)

		val input = DeltakerInput(
			tiltakDeltakerId = Random().nextLong(),
			tiltakgjennomforingId = gjennomforingId,
		)

		val first = deltakerExecutor.execute(NyDeltakerCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		deltakerExecutor.execute(NyDeltakerCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.timestamp.isAfter(first.output?.timestamp) }
	}

	@Test
	fun whenOrdsClientThrowsShouldRetry() {
		val gjennomforingId = Random().nextLong()
		val personId = 123456789L
		setupTiltakOgGjennomforing(gjennomforingId)

		OrdsClientMock.fnrHandlers["$personId"] = { throw SocketTimeoutException() }

		val input = DeltakerInput(
			tiltakDeltakerId = Random().nextLong(),
			tiltakgjennomforingId = gjennomforingId,
			personId = personId
		)

		deltakerExecutor.execute(NyDeltakerCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.result { _, translation, _ -> translation shouldBe null }
			.result { _, _, output -> output shouldBe null }
	}

	@Test
	fun shouldRetryWhenOrdsClientReturnNull() {
		val gjennomforingId = Random().nextLong()
		val personId = 123456789L
		setupTiltakOgGjennomforing(gjennomforingId)

		OrdsClientMock.fnrHandlers["$personId"] = { null }

		val input = DeltakerInput(
			tiltakDeltakerId = Random().nextLong(),
			tiltakgjennomforingId = gjennomforingId,
			personId = personId
		)

		deltakerExecutor.execute(NyDeltakerCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.result { _, translation, _ -> translation shouldBe null }
			.result { _, _, output -> output shouldBe null }
	}

	fun setupTiltakOgGjennomforing(gjennomforingId: Long): GjennomforingResult {
		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		return gjennomforingExecutor.execute(NyGjennomforingCommand(GjennomforingInput(gjennomforingId = gjennomforingId)))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
	}

}

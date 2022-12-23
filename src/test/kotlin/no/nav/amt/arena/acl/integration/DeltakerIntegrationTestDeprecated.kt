package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.integration.commands.deltaker.DeleteDeltakerCommand
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerInput
import no.nav.amt.arena.acl.integration.commands.deltaker.NyDeltakerCommand
import no.nav.amt.arena.acl.integration.commands.deltaker.OppdaterDeltakerCommand
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingResult
import no.nav.amt.arena.acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.amt.arena.acl.integration.commands.sak.NySakCommand
import no.nav.amt.arena.acl.integration.commands.sak.SakInput
import no.nav.amt.arena.acl.integration.commands.tiltak.NyttTiltakCommand
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class DeltakerIntegrationTestDeprecated : IntegrationTestBase() {

	@BeforeEach
	fun setup() {
		mockArenaOrdsProxyHttpServer.mockHentVirksomhetsnummer("0", "12345")
		mockArenaOrdsProxyHttpServer.mockHentFnr("0", "12345")
	}

	@Test
	fun `ingest deltaker`() {
		val gjennomforingId = Random().nextLong()
		val deltakerId = Random().nextLong()

		val gjennomforingResult = ingestGjennomforingOgTiltak(gjennomforingId)

		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = deltakerId,
			tiltakgjennomforingId = gjennomforingId,
			innsokBegrunnelse = "begrunnelse"
		)

		val result = deltakerExecutor.execute(NyDeltakerCommand(deltakerInput))
		result.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.operation shouldBe AmtOperation.CREATED }
			.result { _, translation, output -> translation!!.amtId shouldBe output!!.payload!!.id }
			.outgoingPayload { it.gjennomforingId shouldBe gjennomforingResult.output!!.payload!!.id }
			.outgoingPayload { it.innsokBegrunnelse shouldBe "begrunnelse" }
			.outgoingPayload { it.status shouldBe AmtDeltaker.Status.DELTAR }
	}

	@Test
	fun `ingest deltaker - deltaker ingestet med andre data - oppdaterer deltaker `() {
		val gjennomforingId = Random().nextLong()
		val deltakerId = Random().nextLong()

		ingestGjennomforingOgTiltak(gjennomforingId)

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
	fun `ingest deltaker - lik deltaker allerede lagt på kafka - legger ikke samme melding på kafka`() {
		val gjennomforingId = Random().nextLong()
		ingestGjennomforingOgTiltak(gjennomforingId)

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
	fun `ingest deltaker - ords kaster exception - får status RETRY`() {
		val gjennomforingId = Random().nextLong()
		val personId = 123456789L
		ingestGjennomforingOgTiltak(gjennomforingId)

		mockArenaOrdsProxyHttpServer.mockFailure()

		val input = DeltakerInput(
			tiltakDeltakerId = Random().nextLong(),
			tiltakgjennomforingId = gjennomforingId,
			personId = personId
		)

		deltakerExecutor.execute(NyDeltakerCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.result { _, _, output -> output shouldBe null }
	}

	@Test
	fun `ingest deltaker - ords returnerer null - får status RETRY`() {
		val gjennomforingId = Random().nextLong()
		val personId = 123456789L
		ingestGjennomforingOgTiltak(gjennomforingId)

		mockArenaOrdsProxyHttpServer.mockHentFnr(personId.toString(), null)

		val input = DeltakerInput(
			tiltakDeltakerId = Random().nextLong(),
			tiltakgjennomforingId = gjennomforingId,
			personId = personId
		)

		deltakerExecutor.execute(NyDeltakerCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.result { _, _, output -> output shouldBe null }
	}


	@Test
	fun `slett deltaker - Deltaker blir slettet`() {
		val rnd = Random()
		val gjennomforingId = rnd.nextLong()

		ingestGjennomforingOgTiltak(gjennomforingId)

		val input = DeltakerInput(
			tiltakDeltakerId = rnd.nextLong(),
			tiltakgjennomforingId = gjennomforingId
		)

		val nyDeltakerCommand = NyDeltakerCommand(input)

		val nyResult = deltakerExecutor.execute(nyDeltakerCommand)

		val deleteDeltakerCommand = DeleteDeltakerCommand(input)

		deltakerExecutor.execute(deleteDeltakerCommand)
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.arenaData { it.operation shouldBe AmtOperation.DELETED }
			.output { it.payload?.id shouldBe nyResult.output?.payload?.id }
			.output { it.operation shouldBe AmtOperation.DELETED }
	}

	@Test
	fun `processMessages - deltaker har status RETRY pga manglende gjennomføring - får status HANDLED`() {
		val gjennomforingId = Random().nextLong()

		val input = DeltakerInput(
			tiltakDeltakerId = Random().nextLong(),
			tiltakgjennomforingId = gjennomforingId
		)

		val command = NyDeltakerCommand(input)

		val firstResult = deltakerExecutor.execute(command)
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.arenaData { it.note shouldBe "Venter på at gjennomføring med id=$gjennomforingId skal bli håndtert" }
			.result { _, _, output -> output shouldBe null }

		ingestGjennomforingOgTiltak(gjennomforingId)

		processMessages()

		deltakerExecutor.updateResults(firstResult.position, command)
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.arenaData { it.note shouldBe null }
			.result { _, translation, _ -> translation shouldNotBe null }
			.result { _, _, output -> output shouldNotBe null }
	}

	@Test
	fun `ingest deltaker - tiltak er ikke støttet - får status IGNORED`() {
		val arenaGjennomforingId = Random().nextLong()
		var amtGjennomforingId: UUID? = null

		gjennomforingExecutor.execute(
			NyGjennomforingCommand(
				GjennomforingInput(
					tiltakKode = "IKKE_EKSISTERENDE",
					gjennomforingId = arenaGjennomforingId
				)
			)
		)
			.arenaData { it.ingestStatus shouldBe IngestStatus.IGNORED }
			.translation { amtGjennomforingId = it.amtId }

		deltakerExecutor.execute(
			NyDeltakerCommand(
				DeltakerInput(
					tiltakDeltakerId = Random().nextLong(),
					tiltakgjennomforingId = arenaGjennomforingId
				)
			)
		)
			.arenaData { it.ingestStatus shouldBe IngestStatus.IGNORED }
			.arenaData { it.note shouldBe "Deltaker på en gjennomføring $amtGjennomforingId er ignorert" }
	}

	@Test
	fun `ingest deltaker - deltaker id = 0 - får status INVALID`() {
		val gjennomforingId = Random().nextLong()

		ingestGjennomforingOgTiltak(gjennomforingId)

		deltakerExecutor.execute(
			NyDeltakerCommand(
				DeltakerInput(
					tiltakgjennomforingId = gjennomforingId,
					tiltakDeltakerId = 0
				)
			)
		)
			.arenaData { it.ingestStatus shouldBe IngestStatus.INVALID }
			.arenaData { it.note shouldBe "TILTAKDELTAKER_ID er 0" }
			.result { _, _, output -> output shouldBe null }
	}

	@Test
	fun `ingest deltaker - personId er null - får status INVALID`() {
		val gjennomforingId = Random().nextLong()

		ingestGjennomforingOgTiltak(gjennomforingId)

		deltakerExecutor.execute(
			NyDeltakerCommand(
				DeltakerInput(
					tiltakgjennomforingId = gjennomforingId,
					tiltakDeltakerId = Random().nextLong(),
					personId = null
				)
			)
		)
			.arenaData { it.ingestStatus shouldBe IngestStatus.INVALID }
			.arenaData { it.note shouldBe "PERSON_ID er null" }
			.result { _, _, output -> output shouldBe null }
	}

	@Test
	fun `ingest deltaker - tiltak er ugyldig - får status RETRY`() {
		val gjennomforingId = Random().nextLong()
		val deltakerId = Random().nextLong()

		ingestInvalidGjennomforing(gjennomforingId)

		val deltakerInput = DeltakerInput(tiltakDeltakerId = deltakerId, tiltakgjennomforingId = gjennomforingId)

		deltakerExecutor.execute(NyDeltakerCommand(deltakerInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.arenaData { it.note shouldBe "Venter på at gjennomføring med id=${gjennomforingId} skal bli håndtert" }
			.output shouldBe null
	}

	@Test
	fun `processMessages - deltaker er RETRY pga ugyldig gjennomføring før ny gyldig gjennomføring blir ingested - deltaker får status HANDLED`() {
		val gjennomforingId = Random().nextLong()

		ingestInvalidGjennomforing(gjennomforingId)

		val input = DeltakerInput(
			tiltakDeltakerId = Random().nextLong(),
			tiltakgjennomforingId = gjennomforingId
		)

		val command = NyDeltakerCommand(input)

		val firstResult = deltakerExecutor.execute(command)
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.arenaData { it.note shouldBe "Venter på at gjennomføring med id=$gjennomforingId skal bli håndtert" }


		ingestGjennomforingOgTiltak(gjennomforingId)

		processMessages()
		deltakerExecutor.updateResults(firstResult.position, command)
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.arenaData { it.note shouldBe null }
			.result { _, translation, _ -> translation shouldNotBe null }
			.result { _, _, output -> output shouldNotBe null }
	}

	fun ingestGjennomforingOgTiltak(gjennomforingId: Long): GjennomforingResult {
		val gjennomforingInput = GjennomforingInput(gjennomforingId = gjennomforingId)
		val sakInput = SakInput(sakId = gjennomforingInput.sakId)

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		sakExecutor.execute(NySakCommand(sakInput, gjennomforingId))

		return gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
	}

	fun ingestInvalidGjennomforing(gjennomforingId: Long): GjennomforingResult {
		val gjennomforingCmd = NyGjennomforingCommand(
			GjennomforingInput(
				gjennomforingId = gjennomforingId,
				arbeidsgiverIdArrangor = null
			)
		)

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }


		return gjennomforingExecutor.execute(gjennomforingCmd)
			.arenaData { it.ingestStatus shouldBe IngestStatus.INVALID }
	}
}

package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingInput
import no.nav.amt.arena.acl.integration.commands.gjennomforing.NyGjennomforingCommand
import no.nav.amt.arena.acl.integration.commands.sak.NySakCommand
import no.nav.amt.arena.acl.integration.commands.sak.SakInput
import no.nav.amt.arena.acl.integration.commands.tiltak.NyttTiltakCommand
import no.nav.amt.arena.acl.mocks.OrdsClientMock
import org.junit.jupiter.api.Test
import java.util.*

class GjennomforingIntegrationTests : IntegrationTestBase() {

	@Test
	fun `Konsumer gjennomføring - gyldig gjennomføring - ingestes uten feil`() {
		val gjennomforingInput = GjennomforingInput(
			gjennomforingId = Random().nextLong()
		)

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.operation shouldBe AmtOperation.CREATED }
			.result { _, translation, output -> translation!!.amtId shouldBe output!!.payload!!.id }
	}

	@Test
	fun `Konsumer gjennomføring - ignorert tiltak - settes til ignored`() {

		val input = GjennomforingInput(
			gjennomforingId = Random().nextLong(),
			tiltakKode = "DETTE_TILTAKET_FINNES_IKKE"
		)

		gjennomforingExecutor.execute(NyGjennomforingCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.IGNORED }
			.translation { it.ignored shouldBe true }
			.result { _, _, output -> output shouldBe null }
	}

	@Test
	fun `Konsumer tiltak - tiltak har ventende gjennomføringer - gjennomføringer prosessert`() {
		val tiltakNavn = UUID.randomUUID().toString()

		val command = NyGjennomforingCommand(
			GjennomforingInput(
				gjennomforingId = Random().nextLong()
			)
		)

		val firstResult = gjennomforingExecutor.execute(command)
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.arenaData { it.ingestAttempts shouldBe 0 }
			.arenaData { it.lastAttempted shouldBe null }
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

	@Test
	fun `Konsumer gjennomføring - Feilet på første forsøk - Skal settes til RETRY`() {
		val virksomhetsId = 456785618L

		tiltakExecutor.execute(NyttTiltakCommand())

		OrdsClientMock.virksomhetsHandler["$virksomhetsId"] = { throw RuntimeException() }

		val input = GjennomforingInput(
			gjennomforingId = Random().nextLong(),
			arbeidsgiverIdArrangor = virksomhetsId
		)

		gjennomforingExecutor.execute(NyGjennomforingCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.RETRY }
			.result { _, translation, _ -> translation shouldBe null }
			.result { _, _, output -> output shouldBe null }

	}

	@Test
	fun `Konsumer gjennomføring - arbgiverid er null - status settes til INVALID`() {
		tiltakExecutor.execute(NyttTiltakCommand())

		val input = GjennomforingInput(
			gjennomforingId = Random().nextLong(),
			arbeidsgiverIdArrangor = null
		)

		gjennomforingExecutor.execute(NyGjennomforingCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.INVALID }
			.arenaData { it.note shouldBe "ARBGIV_ID_ARRANGOR er null" }
			.result { _, translation, _ -> translation shouldBe null }
			.result { _, _, output -> output shouldBe null }
	}

	@Test
	fun `Konsumer gjennomføring - lokaltnavn er null - status settes til INVALID`() {
		tiltakExecutor.execute(NyttTiltakCommand())

		val input = GjennomforingInput(
			gjennomforingId = Random().nextLong(),
			navn = null
		)

		gjennomforingExecutor.execute(NyGjennomforingCommand(input))
			.arenaData { it.ingestStatus shouldBe IngestStatus.INVALID }
			.arenaData { it.note shouldBe "LOKALTNAVN er null" }
			.result { _, translation, _ -> translation shouldBe null }
			.result { _, _, output -> output shouldBe null }
	}

	@Test
	fun `Konsumer gjennomføring - med sakId, sak record er allerede konsumert - produserer record med sakid`() {
		val gjennomforingInput = GjennomforingInput()
		val sakInput = SakInput(sakId = gjennomforingInput.sakId)

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		sakExecutor.execute(NySakCommand(sakInput, gjennomforingInput.gjennomforingId))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.result {_, _, _, storedSak -> storedSak.lopenr shouldBe sakInput.lopenr }
			.result {_, _, _, storedSak -> storedSak.aar shouldBe sakInput.aar }
			.result {_, _, _, storedSak -> storedSak.ansvarligEnhetId shouldBe sakInput.ansvarligEnhetId }
			.result {_, _, _, storedSak -> storedSak.arenaSakId shouldBe sakInput.sakId }
			.result {_, _, _, storedSak -> storedSak.createdAt shouldNotBe null}

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.operation shouldBe AmtOperation.CREATED }
			.result { _, translation, output -> translation!!.amtId shouldBe output!!.payload!!.id }
			.output { it.payload!!.lopenr shouldBe sakInput.lopenr}
			.output { it.payload!!.opprettetAar shouldBe sakInput.aar}
			.output { it.payload!!.ansvarligNavEnhetId shouldBe sakInput.ansvarligEnhetId }

	}

	@Test
	fun `Konsumer sak - gjennomføring er allerede konsumert - produserer gjennomføring med løpenummer`() {
		val gjennomforingInput = GjennomforingInput()
		val sakInput = SakInput(sakId = gjennomforingInput.sakId)

		tiltakExecutor.execute(NyttTiltakCommand())
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }

		gjennomforingExecutor.execute(NyGjennomforingCommand(gjennomforingInput))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.operation shouldBe AmtOperation.CREATED }
			.result { _, translation, output -> translation!!.amtId shouldBe output!!.payload!!.id }

		sakExecutor.execute(NySakCommand(sakInput, gjennomforingInput.gjennomforingId))
			.arenaData { it.ingestStatus shouldBe IngestStatus.HANDLED }
			.output { it.operation shouldBe AmtOperation.MODIFIED }
			.output { it.payload!!.lopenr shouldBe sakInput.lopenr }
			.output { it.payload!!.opprettetAar shouldBe sakInput.aar }
			.output { it.payload!!.ansvarligNavEnhetId shouldBe sakInput.ansvarligEnhetId }
			.output { it.payload!!.tiltak.kode shouldBe gjennomforingInput.tiltakKode}
			.output { it.payload!!.tiltak.navn shouldNotBe null}


	}
}

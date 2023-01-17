package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.GjennomforingArenaData
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.RetryArenaMessageProcessorService
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.*

@Ignore
class RetryArenaMessageProcessorServiceTest : IntegrationTestBase() {

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var arenaDataRepository: ArenaDataRepository

	@Autowired
	lateinit var retryArenaMessageProcessorService: RetryArenaMessageProcessorService
	val gjennomforingId = 5435345L

	@Test
	fun `processMessages - deltaker har status RETRY pga manglende gjennomføring - får status HANDLED når gjennomføring er ingestet`() {

		val deltakere: MutableList<Pair<String, ArenaDeltaker>> = mutableListOf()
		var pos = 1

		repeat(3) {
			val currentPos = pos++.toString()
			val deltaker = publiserDeltaker(currentPos)
			deltakere.add(Pair(currentPos, deltaker))
		}

		publiserGjennomforing(pos++.toString())

		retryArenaMessageProcessorService.processMessages(2)

		deltakere.forEach {deltaker ->
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, deltaker.first)
			println(deltaker)
			arenaData!!.ingestStatus shouldBe IngestStatus.HANDLED
		}

	}

	private fun publiserDeltaker(pos: String): ArenaDeltaker {
		val deltaker = KafkaMessageCreator.baseDeltaker(
			tiltakGjennomforingId = gjennomforingId,
		)
		mockArenaOrdsProxyHttpServer.mockHentFnr(deltaker.PERSON_ID!!, (1..Long.MAX_VALUE).random().toString())

		kafkaMessageSender.publiserArenaDeltaker(
			deltaker.TILTAKDELTAKER_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = deltaker, opPos = pos))
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.RETRY

		}

		return deltaker

	}
	private fun publiserGjennomforing(pos: String) {
		val gjennomforing = KafkaMessageCreator.baseGjennomforing(
			arenaGjennomforingId = gjennomforingId,
			arbgivIdArrangor = 68968L,
			datoFra = LocalDateTime.now().minusDays(3),
			datoTil = LocalDateTime.now().plusDays(3),
		)

		val gjennomforingIdMR = UUID.randomUUID()
		val gjennomforingArenaData = GjennomforingArenaData(
			opprettetAar = 2022,
			lopenr = 123,
			virksomhetsnummer = "999888777",
			ansvarligNavEnhetId = "1234",
			status = "GJENNOMFOR",
		)
		mockMulighetsrommetApiServer.mockHentGjennomforingId(gjennomforingId, gjennomforingIdMR)
		mockMulighetsrommetApiServer.mockHentGjennomforingArenaData(gjennomforingIdMR, gjennomforingArenaData)

		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforingId,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing, opPos = pos))
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_GJENNOMFORING_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.HANDLED

		}

	}

}

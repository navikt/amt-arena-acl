package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.GjennomforingArenaData
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.GjennomforingRepository
import no.nav.amt.arena.acl.schedule.ArenaDataSchedules
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils
import org.junit.Ignore
import org.junit.jupiter.api.BeforeEach
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
	lateinit var arenaDataSchedules: ArenaDataSchedules

	@Autowired
	lateinit var gjennomforingRepository: GjennomforingRepository

	val amtGjennomforingId = UUID.randomUUID()
	val gjennomforing = KafkaMessageCreator.baseGjennomforing(
		arenaGjennomforingId = 5435345,
		arbgivIdArrangor = 68968L,
		datoFra = LocalDateTime.now().minusDays(3),
		datoTil = LocalDateTime.now().plusDays(3),
	)


	@BeforeEach
	fun setup() {
		val gjennomforingArenaData = GjennomforingArenaData(
			opprettetAar = 2022,
			lopenr = 123,
			virksomhetsnummer = "999888777",
			ansvarligNavEnhetId = "1234",
			status = "GJENNOMFOR",
		)
		mockMulighetsrommetApiServer.mockHentGjennomforingId(gjennomforing.TILTAKGJENNOMFORING_ID, amtGjennomforingId)
		mockMulighetsrommetApiServer.mockHentGjennomforingArenaData(amtGjennomforingId, gjennomforingArenaData)
	}

/*
	@Test
	fun `processMessages - deltaker har status RETRY pga manglende gjennomføring - får status HANDLED når gjennomføring er ingestet`() {
		val deltaker1 = createDeltaker(gjennomforing.TILTAKGJENNOMFORING_ID)
		val deltaker2 = createDeltaker(gjennomforing.TILTAKGJENNOMFORING_ID)
		val pos1 = "12"
		val pos2 = "34"

		mockArenaOrdsProxyHttpServer.mockHentFnr(deltaker1.PERSON_ID!!, (0..10).random().toString())
		mockArenaOrdsProxyHttpServer.mockHentFnr(deltaker2.PERSON_ID!!, (0..10).random().toString())

		kafkaMessageSender.publiserArenaDeltaker(
			deltaker1.TILTAKDELTAKER_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = deltaker1, opPos = pos1))
		)
		kafkaMessageSender.publiserArenaDeltaker(
			deltaker2.TILTAKDELTAKER_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = deltaker2, opPos = pos2))
		)

		AsyncUtils.eventually {
			val arenaData1 = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos1)
			val arenaData2 = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos2)

			arenaData1!!.ingestStatus shouldBe IngestStatus.RETRY
			arenaData2!!.ingestStatus shouldBe IngestStatus.RETRY

		}

		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforing.TILTAKGJENNOMFORING_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing))
		)

		AsyncUtils.eventually {
			val gjennomforingResult = gjennomforingRepository.get(gjennomforing.TILTAKGJENNOMFORING_ID.toString())
			gjennomforingResult!!.isValid shouldBe true
		}

		arenaDataSchedules.processArenaMessages()

		val arenaData1 = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos1)
		val arenaData2 = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos2)

		arenaData1!!.ingestStatus shouldBe IngestStatus.HANDLED
		arenaData1.note shouldBe null

		arenaData2!!.ingestStatus shouldBe IngestStatus.HANDLED
		arenaData2.note shouldBe null

	}*/

}

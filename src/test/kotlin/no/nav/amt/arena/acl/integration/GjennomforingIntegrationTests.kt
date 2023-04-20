package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.DirtyContextBeforeAndAfterClassTestExecutionListener
import no.nav.amt.arena.acl.utils.JsonUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestExecutionListeners
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@TestExecutionListeners(
	listeners = [DirtyContextBeforeAndAfterClassTestExecutionListener::class],
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class GjennomforingIntegrationTests : IntegrationTestBase() {

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var gjennomforingService: GjennomforingService

	@Autowired
	lateinit var arenaDataRepository: ArenaDataRepository

	@BeforeEach
	fun setup() {
		mockArenaOrdsProxyHttpServer.mockHentVirksomhetsnummer("0", "12345")
	}

	@Test
	fun `Konsumer gjennomføring - gyldig gjennomføring - ingestes uten feil`() {
		val gjennomforing = createGjennomforing()
		val gjennomforingId = UUID.randomUUID()
		mockMulighetsrommetApiServer.mockHentGjennomforingId(gjennomforing.TILTAKGJENNOMFORING_ID, gjennomforingId)
		val pos = (1..Long.MAX_VALUE).random().toString()

		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforing.TILTAKGJENNOMFORING_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing, opPos = pos))
		)
		AsyncUtils.eventually(until = Duration.ofSeconds(10))  {
			val gjennomforingResult = gjennomforingService.get(gjennomforing.TILTAKGJENNOMFORING_ID.toString())
			gjennomforingResult shouldNotBe null
			gjennomforingResult!!.isValid shouldBe true
			gjennomforingResult.isSupported shouldBe true
			gjennomforingResult.id shouldBe gjennomforingId

			arenaDataRepository.get(
				ARENA_GJENNOMFORING_TABLE_NAME,
				AmtOperation.CREATED,
				pos
			)!!.ingestStatus shouldBe IngestStatus.HANDLED
		}

	}

	@Test
	fun `Konsumer gjennomføring - ugyldig gjennomføring - får status invalid`() {
		var gjennomforing = createGjennomforing()
		val gjennomforingId = UUID.randomUUID()
		mockMulighetsrommetApiServer.mockHentGjennomforingId(gjennomforing.TILTAKGJENNOMFORING_ID, gjennomforingId)
		val pos = (1..Long.MAX_VALUE).random().toString()
		gjennomforing = gjennomforing.copy(ARBGIV_ID_ARRANGOR = null)
		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforing.TILTAKGJENNOMFORING_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing, opPos = pos))
		)
		AsyncUtils.eventually(until = Duration.ofSeconds(10))  {
			val gjennomforingResult = gjennomforingService.get(gjennomforing.TILTAKGJENNOMFORING_ID.toString())
			gjennomforingResult shouldNotBe null
			gjennomforingResult!!.isValid shouldBe false
			gjennomforingResult.isSupported shouldBe true
			gjennomforingResult.id shouldBe gjennomforingId

			arenaDataRepository.get(
				ARENA_GJENNOMFORING_TABLE_NAME,
				AmtOperation.CREATED,
				pos
			)!!.ingestStatus shouldBe IngestStatus.HANDLED
		}

	}
	@Test
	fun `Konsumer gjennomføring - tiltakstype er ikke støttet - lagrer med korrekte verdier`() {
		val gjennomforing = createGjennomforing().copy(TILTAKSKODE = "IKKE STØTTET")
		val gjennomforingId = UUID.randomUUID()
		mockMulighetsrommetApiServer.mockHentGjennomforingId(gjennomforing.TILTAKGJENNOMFORING_ID, gjennomforingId)
		val pos = (1..Long.MAX_VALUE).random().toString()

		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforing.TILTAKGJENNOMFORING_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing, opPos = pos))
		)
		AsyncUtils.eventually {
			val gjennomforingResult = gjennomforingService.get(gjennomforing.TILTAKGJENNOMFORING_ID.toString())
			gjennomforingResult shouldNotBe null
			gjennomforingResult!!.isValid shouldBe true
			gjennomforingResult.isSupported shouldBe false
			gjennomforingResult.id shouldBe gjennomforingId

			arenaDataRepository.get(
				ARENA_GJENNOMFORING_TABLE_NAME,
				AmtOperation.CREATED,
				pos
			) shouldBe null
		}


	}
	private fun createGjennomforing(arenaId: Long = 34524534543) = KafkaMessageCreator.baseGjennomforing(
		arenaGjennomforingId = arenaId,
		arbgivIdArrangor = 68968L,
		datoFra = LocalDateTime.now().minusDays(3),
		datoTil = LocalDateTime.now().plusDays(3),
	)
}

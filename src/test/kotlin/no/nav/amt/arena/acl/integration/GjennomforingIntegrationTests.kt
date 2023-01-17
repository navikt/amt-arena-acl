package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.repositories.GjennomforingRepository
import no.nav.amt.arena.acl.utils.DirtyContextBeforeAndAfterClassTestExecutionListener
import no.nav.amt.arena.acl.utils.JsonUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestExecutionListeners
import java.time.Duration
import java.time.LocalDateTime

@TestExecutionListeners(
	listeners = [DirtyContextBeforeAndAfterClassTestExecutionListener::class],
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class GjennomforingIntegrationTests : IntegrationTestBase() {

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var gjennomforingRepository: GjennomforingRepository

	@BeforeEach
	fun setup() {
		mockArenaOrdsProxyHttpServer.mockHentVirksomhetsnummer("0", "12345")
	}

	@Test
	fun `Konsumer gjennomføring - gyldig gjennomføring - ingestes uten feil`() {
		val gjennomforing = createGjennomforing()
		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforing.TILTAKGJENNOMFORING_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing))
		)
		AsyncUtils.eventually(until = Duration.ofSeconds(10))  {
			val gjennomforingResult = gjennomforingRepository.get(gjennomforing.TILTAKGJENNOMFORING_ID.toString())
			gjennomforingResult shouldNotBe null
			gjennomforingResult!!.isValid shouldBe true
		}

	}

	@Test
	fun `Konsumer gjennomføring - ugyldig gjennomføring - får status invalid`() {
		var gjennomforing = createGjennomforing()
		gjennomforing = gjennomforing.copy(ARBGIV_ID_ARRANGOR = null)
		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforing.TILTAKGJENNOMFORING_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing))
		)
		AsyncUtils.eventually(until = Duration.ofSeconds(10))  {
			val gjennomforingResult = gjennomforingRepository.get(gjennomforing.TILTAKGJENNOMFORING_ID.toString())
			gjennomforingResult shouldNotBe null
			gjennomforingResult!!.isValid shouldBe false
		}

	}
}

fun createGjennomforing(arenaId: Long = 34524534543) = KafkaMessageCreator.baseGjennomforing(
	arenaGjennomforingId = arenaId,
	arbgivIdArrangor = 68968L,
	datoFra = LocalDateTime.now().minusDays(3),
	datoTil = LocalDateTime.now().plusDays(3),
)

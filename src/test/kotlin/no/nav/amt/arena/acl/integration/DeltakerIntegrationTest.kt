package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.GjennomforingArenaData
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Tiltakstype
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageConsumer
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.integration.utils.DateUtils
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.IgnoredArenaDataRepository
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.DirtyContextBeforeAndAfterClassTestExecutionListener
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestExecutionListeners
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@TestExecutionListeners(
	listeners = [DirtyContextBeforeAndAfterClassTestExecutionListener::class],
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class DeltakerIntegrationTest : IntegrationTestBase() {

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var arenaDataRepository: ArenaDataRepository

	@Autowired
	lateinit var arenaDataIdTranslationRepository: ArenaDataIdTranslationRepository

	@Autowired
	lateinit var ignoredArenaDataRepository: IgnoredArenaDataRepository


	companion object {
		@JvmStatic
		@DynamicPropertySource
		fun startEnvironment(registry: DynamicPropertyRegistry) {
			setupEnvironment(registry)
			registry.add("toggle.hent_gjennomforing_fra_mulighetsrommet") { "true" }
		}
	}

	val deltakerRegistertDato = LocalDateTime.now()
	val deltakerStatusEndretDato = LocalDateTime.now()
	val deltakerStartDato = LocalDate.now().plusDays(1)
	val deltakerSluttDato = LocalDate.now().plusMonths(6)

	val baseDeltaker = KafkaMessageCreator.baseDeltaker(
		arenaDeltakerId = 789,
		personId = 456,
		tiltakGjennomforingId = 123,
		registrertDato = deltakerRegistertDato,
		startDato = deltakerStartDato,
		sluttDato = deltakerSluttDato,
		datoStatusEndring = deltakerStatusEndretDato,
	)

	val gjennomforingId = UUID.randomUUID()
	val gjennomforing = Gjennomforing(
		id = gjennomforingId,
		tiltak = Tiltakstype(
			id = UUID.randomUUID(),
			navn = "Oppfolging",
			arenaKode = "INDOPPFAG",
		),
		navn = "Navn",
		startDato = LocalDate.now(),
		sluttDato = LocalDate.now().plusMonths(6),
	)
	val gjennomforingArenaData = GjennomforingArenaData(
		opprettetAar = 2022,
		lopenr = 123,
		virksomhetsnummer = "999888777",
		ansvarligNavEnhetId = "1234",
		status = "GJENNOMFOR",
	)

	val fnr = "123456789"

	@Test
	fun `ingest deltaker`() {

		arenaDataIdTranslationRepository.insert(ArenaDataIdTranslationDbo(
			UUID.randomUUID(),
			ARENA_GJENNOMFORING_TABLE_NAME,
			"123",
		))

		mockMulighetsrommetApiServer.mockHentGjennomforingId("123", gjennomforingId)
		mockMulighetsrommetApiServer.mockHentGjennomforing(gjennomforingId, gjennomforing)
		mockMulighetsrommetApiServer.mockHentGjennomforingArenaData(gjennomforingId, gjennomforingArenaData)

		mockArenaOrdsProxyHttpServer.mockHentFnr("456", fnr)

		kafkaMessageSender.publiserArenaDeltaker("789", toJsonString(KafkaMessageCreator.opprettArenaDeltaker(baseDeltaker)))

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldNotBe null

			val deltaker = fromJsonString<AmtKafkaMessageDto<AmtDeltaker>>(deltakerRecord!!.value())
			deltaker.type shouldBe PayloadType.DELTAKER
			deltaker.operation shouldBe AmtOperation.CREATED

			val payload = deltaker.payload!!
			payload.personIdent shouldBe fnr
			payload.gjennomforingId shouldBe gjennomforingId
			payload.status shouldBe AmtDeltaker.Status.VENTER_PA_OPPSTART
			payload.dagerPerUke shouldBe baseDeltaker.ANTALL_DAGER_PR_UKE
			payload.innsokBegrunnelse shouldBe baseDeltaker.BEGRUNNELSE_INNSOKT
			payload.prosentDeltid shouldBe baseDeltaker.PROSENT_DELTID
			payload.sluttDato shouldBe deltakerSluttDato
			payload.startDato shouldBe deltakerStartDato
			payload.statusAarsak shouldBe null
			DateUtils.isEqual(payload.registrertDato, deltakerRegistertDato) shouldBe true
			DateUtils.isEqual(payload.statusEndretDato!!, deltakerStatusEndretDato) shouldBe true
		}
	}

	@Test
	fun `skal ignorere deltaker på ikke støttet gjennomføring`() {
		val amtGjennomforingId = UUID.randomUUID()

		arenaDataIdTranslationRepository.insert(ArenaDataIdTranslationDbo(
			amtGjennomforingId,
			ARENA_GJENNOMFORING_TABLE_NAME,
			"123",
		))

		ignoredArenaDataRepository.ignore(amtGjennomforingId)

		mockMulighetsrommetApiServer.mockHentGjennomforingId("123", gjennomforingId)
		mockMulighetsrommetApiServer.mockHentGjennomforing(gjennomforingId, gjennomforing.copy(tiltak = gjennomforing.tiltak.copy(arenaKode = "IKKESTOTTET")))
		mockMulighetsrommetApiServer.mockHentGjennomforingArenaData(gjennomforingId, gjennomforingArenaData)

		mockArenaOrdsProxyHttpServer.mockHentFnr("456", fnr)
		val pos = "42"
		kafkaMessageSender.publiserArenaDeltaker("789", toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = baseDeltaker, opPos = pos)))

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData.ingestStatus shouldBe IngestStatus.IGNORED
		}
	}


}

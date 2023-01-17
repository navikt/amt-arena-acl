package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.GjennomforingArenaData
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageConsumer
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.GjennomforingRepository
import no.nav.amt.arena.acl.services.SUPPORTED_TILTAK
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.DirtyContextBeforeAndAfterClassTestExecutionListener
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestExecutionListeners
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
	lateinit var gjennomforingRepository: GjennomforingRepository

	val baseDeltaker = createDeltaker()

	val baseGjennomforing = KafkaMessageCreator.baseGjennomforing(
		arenaGjennomforingId = baseDeltaker.TILTAKGJENNOMFORING_ID,
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

	val fnr = "123456789"


	@BeforeEach
	fun setup() {
		mockMulighetsrommetApiServer.mockHentGjennomforingId(
			baseGjennomforing.TILTAKGJENNOMFORING_ID,
			gjennomforingIdMR
		)
		mockMulighetsrommetApiServer.mockHentGjennomforingArenaData(gjennomforingIdMR, gjennomforingArenaData)
	}

	@Test
	fun `ingest deltaker`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		kafkaMessageSender.publiserArenaGjennomforing(
			baseDeltaker.TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(baseGjennomforing))
		)
		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val gjennomforingResult = gjennomforingRepository.get(baseDeltaker.TILTAKGJENNOMFORING_ID.toString())
			gjennomforingResult shouldNotBe null
			gjennomforingResult!!.isValid shouldBe true
		}

		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(baseDeltaker))
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldNotBe null

			val deltaker = fromJsonString<AmtKafkaMessageDto<AmtDeltaker>>(deltakerRecord!!.value())
			deltaker.type shouldBe PayloadType.DELTAKER
			deltaker.operation shouldBe AmtOperation.CREATED

			val payload = deltaker.payload!!
			payload.validate(baseDeltaker)
			payload.personIdent shouldBe fnr
			payload.gjennomforingId shouldBe gjennomforingIdMR
			payload.status shouldBe AmtDeltaker.Status.VENTER_PA_OPPSTART
			payload.statusAarsak shouldBe null

		}
	}

	@Test
	fun `ingest deltaker - ikke støttet tiltak - skal ignoreres`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingRepository.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), "IKKESTOTTET", true)

		val pos = "42"
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = baseDeltaker, opPos = pos))
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.IGNORED
		}
	}

	@Test
	fun `ingest deltaker - gjennomføring er ugyldig - deltaker får status RETRY`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingRepository.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), false)
		mockMulighetsrommetApiServer.mockHentGjennomforingId(
			baseGjennomforing.TILTAKGJENNOMFORING_ID,
			gjennomforingIdMR
		)

		mockMulighetsrommetApiServer.mockHentGjennomforingArenaData(
			gjennomforingIdMR,
			gjennomforingArenaData.copy(virksomhetsnummer = null)
		)

		val pos = "77"
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = baseDeltaker, opPos = pos))
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.RETRY
		}
	}

	@Test
	fun `ingest deltaker - deltaker ingestet med andre data - oppdaterer deltaker`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)

		gjennomforingRepository.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(baseDeltaker))
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldNotBe null

			val deltaker = fromJsonString<AmtKafkaMessageDto<AmtDeltaker>>(deltakerRecord!!.value())
			deltaker.type shouldBe PayloadType.DELTAKER
			deltaker.operation shouldBe AmtOperation.CREATED

			val payload = deltaker.payload!!
			payload.validate(baseDeltaker)
			payload.personIdent shouldBe fnr
			payload.gjennomforingId shouldBe gjennomforingIdMR
			payload.status shouldBe AmtDeltaker.Status.VENTER_PA_OPPSTART
			payload.statusAarsak shouldBe null

		}
	}

	@Test
	fun `ingest deltaker - uthenting av fnr fra arena feiler - får status RETRY`() {
		val pos = "33"
		mockArenaOrdsProxyHttpServer.mockFailHentFnr(baseDeltaker.PERSON_ID!!)
		gjennomforingRepository.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(baseDeltaker, opPos = pos))
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)

			deltakerRecord shouldBe null
			arenaData!!.ingestStatus shouldBe IngestStatus.RETRY

		}
	}

	@Test
	fun `ingest deltaker - deltaker er ugyldig - får status INVALID`() {
		val pos = "33"
		val deltaker = baseDeltaker.copy(TILTAKDELTAKER_ID = 0)
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingRepository.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(deltaker, opPos = pos))
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)

			deltakerRecord shouldBe null
			arenaData!!.ingestStatus shouldBe IngestStatus.INVALID
		}
	}

	@Test
	fun `slett deltaker - Deltaker blir slettet`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)

		gjennomforingRepository.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		val pos = "42"
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKGJENNOMFORING_ID,
			toJsonString(
				KafkaMessageCreator.opprettArenaDeltaker(
					arenaDeltaker = baseDeltaker,
					opPos = pos,
					opType = "D"
				)
			)
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.DELETED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.HANDLED
		}
	}

	private fun AmtDeltaker.validate(baseDeltaker: ArenaDeltaker) {
		fun parseDate(dateStr: String?): LocalDate? {
			val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			return dateStr?.let { LocalDate.parse(dateStr, dateFormatter) }
		}

		fun parseDateTime(dateStr: String?): LocalDateTime? {
			val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			return dateStr?.let { LocalDateTime.parse(dateStr, dateFormatter) }
		}

		dagerPerUke shouldBe baseDeltaker.ANTALL_DAGER_PR_UKE
		innsokBegrunnelse shouldBe baseDeltaker.BEGRUNNELSE_INNSOKT
		prosentDeltid shouldBe baseDeltaker.PROSENT_DELTID
		sluttDato shouldBe parseDate(baseDeltaker.DATO_TIL)
		startDato shouldBe parseDate(baseDeltaker.DATO_FRA)
		registrertDato shouldBe parseDateTime(baseDeltaker.REG_DATO)
		statusEndretDato shouldBe parseDateTime(baseDeltaker.DATO_STATUSENDRING)
	}

	private fun createDeltaker() = KafkaMessageCreator.baseDeltaker(
		startDato = LocalDate.now().plusDays(1),
		sluttDato = LocalDate.now().plusMonths(6),
	)

}


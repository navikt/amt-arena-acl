package no.nav.amt.arena.acl.integration

import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.Gjennomforing
import no.nav.amt.arena.acl.domain.db.ArenaDataUpsertInput
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforing
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageConsumer
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.services.SUPPORTED_TILTAK
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.DirtyContextBeforeAndAfterClassTestExecutionListener
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestExecutionListeners
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

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
	lateinit var gjennomforingService: GjennomforingService

	val baseDeltaker = createDeltaker()

	val baseGjennomforing = KafkaMessageCreator.baseGjennomforing(
		arenaGjennomforingId = baseDeltaker.TILTAKGJENNOMFORING_ID,
		arbgivIdArrangor = 68968L,
		datoFra = LocalDateTime.now().minusDays(3),
		datoTil = LocalDateTime.now().plusDays(3),
	)

	val gjennomforingIdMR = UUID.randomUUID()
	val gjennomforingMRData = no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing(
		id = gjennomforingIdMR,
		tiltakstype =  no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing.Tiltakstype(
			id = UUID.randomUUID(),
			navn = "Navn på tiltak",
			arenaKode = "INDOPPFAG"
		),
		navn = "Navn på gjennomføring",
	 	startDato = LocalDate.now(),
		sluttDato = LocalDate.now().plusDays(3),
		status = no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing.Status.GJENNOMFORES,
		virksomhetsnummer = "999888777",
		oppstart = no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing.Oppstartstype.LOPENDE
	)

	val fnr = "123456789"

	@BeforeEach
	fun setup() {
		setupMocks()
	}

	fun setupMocks() {
		mockMulighetsrommetApiServer.reset()
		mockArenaOrdsProxyHttpServer.reset()
		mockMulighetsrommetApiServer.mockHentGjennomforingId(
			baseGjennomforing.TILTAKGJENNOMFORING_ID,
			gjennomforingIdMR
		)
		mockMulighetsrommetApiServer.mockHentGjennomforingData(gjennomforingIdMR, gjennomforingMRData)
	}

	@Test
	fun `ingest deltaker`() {
		val gjennomforingArenaId = baseDeltaker.TILTAKGJENNOMFORING_ID.toString()
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)

		baseGjennomforing.publiser {
			it shouldNotBe null
			it!!.isValid shouldBe true
		}

		baseDeltaker.publiserOgValiderOutput {
			it.status shouldBe AmtDeltaker.Status.VENTER_PA_OPPSTART
			it.statusAarsak shouldBe null

			val gjennomforing = gjennomforingService.get(gjennomforingArenaId)
			gjennomforing!!.id shouldBe gjennomforingIdMR
		}
	}

	@Disabled
	@Test
	fun `ingest deltaker - forrige melding på deltaker er ikke ingestet - feiler med status RETRY`() {
		val pos1 = "33"
		val pos2 = "35"

		mockArenaOrdsProxyHttpServer.mockFailHentFnr(baseDeltaker.PERSON_ID!!)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(baseDeltaker, opPos = pos1))
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos1)

			deltakerRecord shouldBe null
			arenaData!!.ingestStatus shouldBe IngestStatus.RETRY

		}
		setupMocks()
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		val endretDeltaker = baseDeltaker.copy(DELTAKERSTATUSKODE = "AKTUELL")
		kafkaMessageSender.publiserArenaDeltaker(
			endretDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(endretDeltaker, opPos = pos2))
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos2)

			deltakerRecord shouldBe null
			arenaData!!.ingestStatus shouldBe IngestStatus.RETRY
			arenaData.note shouldBe "Forrige melding på deltaker med id=${baseDeltaker.TILTAKDELTAKER_ID} er ikke håndtert enda"

		}
	}


	@Test
	fun `ingest deltaker - ikke støttet tiltak - skal ignoreres`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), "IKKESTOTTET", true)

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
	fun `ingest deltaker - gjennomføring er ugyldig - deltaker får status WAITING`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), false)

		val pos = "77"
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = baseDeltaker, opPos = pos))
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.WAITING
		}
	}

	@Test
	fun `ingest deltaker - deltaker ingestet med andre data - oppdaterer deltaker`() {
		val endretDeltaker = KafkaMessageCreator.baseDeltaker(
			arenaDeltakerId = baseDeltaker.TILTAKDELTAKER_ID,
			personId = baseDeltaker.PERSON_ID!!,
			gjennomforingId = baseDeltaker.TILTAKGJENNOMFORING_ID,
			startDato = LocalDate.now().minusDays(1),
			sluttDato = LocalDate.now().plusMonths(6),
		)

		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		baseDeltaker.publiserOgValiderOutput {
			it.status shouldBe AmtDeltaker.Status.VENTER_PA_OPPSTART

		}
		endretDeltaker.publiserOgValiderOutput {
			it.status shouldBe AmtDeltaker.Status.DELTAR
		}

	}

	@Test
	fun `ingest deltaker - uthenting av fnr fra arena feiler - får status RETRY`() {
		val pos = "33"
		mockArenaOrdsProxyHttpServer.mockFailHentFnr(baseDeltaker.PERSON_ID!!)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
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
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
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
	fun `ingest deltaker - dagerPerUke kan ha desimaler`() {
		val pos = "33"
		val deltaker = baseDeltaker.copy(ANTALL_DAGER_PR_UKE = 2.5f)

		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(deltaker, opPos = pos))
		)

		deltaker.publiserOgValiderOutput {
			it.dagerPerUke shouldBe 2.5f
		}
	}

	@Disabled
	@Test
	fun `ingest deltaker - siste melding for samme deltaker var behandlet for mindre enn 500ms siden - venter litt før den behandles`() {
		val deltaker = baseDeltaker.copy(TILTAKDELTAKER_ID = 0)
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		val ingestedTimestamp = LocalDateTime.now()

		arenaDataRepository.upsert(ArenaDataUpsertInput(
			arenaTableName = ARENA_DELTAKER_TABLE_NAME,
			arenaId = deltaker.TILTAKDELTAKER_ID.toString(),
			operation = AmtOperation.CREATED,
			operationPosition = "66",
			operationTimestamp = LocalDateTime.now().minusDays(1),
			ingestStatus = IngestStatus.HANDLED,
			ingestedTimestamp = ingestedTimestamp,
		))

		kafkaMessageSender.publiserArenaDeltaker(
			deltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(deltaker, opPos = "67", opType = "U"))
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.MODIFIED, "67")

			arenaData!!.ingestedTimestamp!! shouldBeAfter ingestedTimestamp.plus(Duration.ofMillis(500))
		}

	}


	@Test
	fun `slett deltaker - deltaker blir ikke slettet, melding blir handled`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)

		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

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
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldBe null
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
		innsokBegrunnelse shouldBe baseDeltaker.BEGRUNNELSE_BESTILLING
		prosentDeltid shouldBe baseDeltaker.PROSENT_DELTID
		sluttDato shouldBe parseDate(baseDeltaker.DATO_TIL)
		startDato shouldBe parseDate(baseDeltaker.DATO_FRA)
		registrertDato shouldBe parseDateTime(baseDeltaker.REG_DATO)
	}

	private fun createDeltaker() = KafkaMessageCreator.baseDeltaker(
		startDato = LocalDate.now().plusDays(1),
		sluttDato = LocalDate.now().plusMonths(6),
	)

	fun ArenaGjennomforing.publiser(customAssertions: (payload: Gjennomforing?) -> Unit) {
		kafkaMessageSender.publiserArenaGjennomforing(
			TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(this))
		)
		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val gjennomforingResult = gjennomforingService.get(TILTAKGJENNOMFORING_ID.toString())
			customAssertions(gjennomforingResult)
		}
	}

	fun ArenaDeltaker.publiserOgValiderOutput(customAssertions: (payload: AmtDeltaker) -> Unit) {
		kafkaMessageSender.publiserArenaDeltaker(
			TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(this))
		)

		AsyncUtils.eventually {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldNotBe null

			val deltakerResult = fromJsonString<AmtKafkaMessageDto<AmtDeltaker>>(deltakerRecord!!.value())
			deltakerResult.type shouldBe PayloadType.DELTAKER
			deltakerResult.operation shouldBe AmtOperation.CREATED

			val payload = deltakerResult.payload!!
			payload.validate(this)
			payload.personIdent shouldBe fnr
			payload.gjennomforingId shouldBe gjennomforingIdMR
			customAssertions(payload)

		}
	}
}



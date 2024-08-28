package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.Gjennomforing
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforing
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaHistDeltaker
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageConsumer
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.services.SUPPORTED_TILTAK
import no.nav.amt.arena.acl.utils.ARENA_HIST_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.DirtyContextBeforeAndAfterClassTestExecutionListener
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import org.junit.Ignore
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
@Disabled
class HistDeltakerIntegrationTest : IntegrationTestBase() {

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
		mockAmtTiltakServer.reset()
		mockMulighetsrommetApiServer.mockHentGjennomforingId(
			baseGjennomforing.TILTAKGJENNOMFORING_ID,
			gjennomforingIdMR
		)
		mockMulighetsrommetApiServer.mockHentGjennomforingData(gjennomforingIdMR, gjennomforingMRData)
	}

	@Ignore
	@Test
	fun `ingest deltaker - ingen deltakelser i amt-tiltak - publiserer deltaker`() {
		val gjennomforingArenaId = baseDeltaker.TILTAKGJENNOMFORING_ID.toString()
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = null,
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL)
		)

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

	@Ignore
	@Test
	fun `ingest deltaker - har deltakelse i amt-tiltak, annen gjennomforing - publiserer deltaker`() {
		val gjennomforingArenaId = baseDeltaker.TILTAKGJENNOMFORING_ID.toString()
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = UUID.randomUUID(),
			gjennomforingId = UUID.randomUUID(),
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL)
		)

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

	@Ignore
	@Test
	fun `ingest deltaker - har deltakelse i amt-tiltak, samme gjennomforing, andre datoer - publiserer deltaker`() {
		val gjennomforingArenaId = baseDeltaker.TILTAKGJENNOMFORING_ID.toString()
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = UUID.randomUUID(),
			gjennomforingId = gjennomforingIdMR,
			startdato = LocalDate.now().minusDays(1),
			sluttdato = LocalDate.now().plusMonths(3)
		)

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

	@Ignore
	@Test
	fun `ingest deltaker - har deltakelse i amt-tiltak, samme gjennomforing og datoer - publiserer ikke deltaker`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = UUID.randomUUID(),
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL)
		)

		baseGjennomforing.publiser {
			it shouldNotBe null
			it!!.isValid shouldBe true
		}

		val pos = "233"
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(baseDeltaker, opPos = pos))
		)
		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.IGNORED
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldBe null
		}
	}

	@Ignore
	@Test
	fun `ingest deltaker - slettemelding - deltaker blir ikke slettet, melding blir ignored`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = null,
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL)
		)

		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		val pos = "42"
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.TILTAKGJENNOMFORING_ID,
			toJsonString(
				KafkaMessageCreator.opprettArenaHistDeltaker(
					arenaDeltaker = baseDeltaker,
					opPos = pos,
					opType = "D"
				)
			)
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.DELETED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.IGNORED
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldBe null
		}
	}

	@Ignore
	@Test
	fun `ingest deltaker - ikke stottet tiltak - skal ignoreres`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = null,
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL)
		)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), "IKKESTOTTET", true)

		val pos = "42"
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(arenaDeltaker = baseDeltaker, opPos = pos))
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.IGNORED
		}
	}

	@Ignore
	@Test
	fun `ingest deltaker - gjennomforing er ugyldig - deltaker far status WAITING`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = null,
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL)
		)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), false)

		val pos = "77"
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(arenaDeltaker = baseDeltaker, opPos = pos))
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.WAITING
		}
	}

	@Ignore
	@Test
	fun `ingest deltaker - uthenting av fnr fra arena feiler - status RETRY`() {
		val pos = "33"
		mockArenaOrdsProxyHttpServer.mockFailHentFnr(baseDeltaker.PERSON_ID!!)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = null,
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL)
		)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(baseDeltaker, opPos = pos))
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)

			deltakerRecord shouldBe null
			arenaData!!.ingestStatus shouldBe IngestStatus.RETRY

		}
	}

	private fun AmtDeltaker.validate(baseDeltaker: ArenaHistDeltaker) {
		dagerPerUke shouldBe baseDeltaker.ANTALL_DAGER_PR_UKE
		innsokBegrunnelse shouldBe baseDeltaker.BEGRUNNELSE_BESTILLING
		prosentDeltid shouldBe baseDeltaker.PROSENT_DELTID
		sluttDato shouldBe parseDate(baseDeltaker.DATO_TIL)
		startDato shouldBe parseDate(baseDeltaker.DATO_FRA)
		registrertDato shouldBe parseDateTime(baseDeltaker.REG_DATO)
	}

	private fun createDeltaker() = KafkaMessageCreator.baseHistDeltaker(
		startDato = LocalDate.now().plusDays(1),
		sluttDato = LocalDate.now().plusMonths(6),
	)

	private fun ArenaGjennomforing.publiser(customAssertions: (payload: Gjennomforing?) -> Unit) {
		kafkaMessageSender.publiserArenaGjennomforing(
			TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(this))
		)
		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val gjennomforingResult = gjennomforingService.get(TILTAKGJENNOMFORING_ID.toString())
			customAssertions(gjennomforingResult)
		}
	}

	private fun ArenaHistDeltaker.publiserOgValiderOutput(customAssertions: (payload: AmtDeltaker) -> Unit) {
		kafkaMessageSender.publiserArenaHistDeltaker(
			HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(this))
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

	private fun parseDate(dateStr: String?): LocalDate? {
		val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		return dateStr?.let { LocalDate.parse(dateStr, dateFormatter) }
	}

	private fun parseDateTime(dateStr: String?): LocalDateTime? {
		val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		return dateStr?.let { LocalDateTime.parse(dateStr, dateFormatter) }
	}
}

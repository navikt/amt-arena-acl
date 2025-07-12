package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.clients.amttiltak.DeltakerStatusDto
import no.nav.amt.arena.acl.domain.Gjennomforing
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforing
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaHistDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageConsumer
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.repositories.ArenaDataHistIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.DeltakerInsertDbo
import no.nav.amt.arena.acl.repositories.DeltakerRepository
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.services.SUPPORTED_TILTAK
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_HIST_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import no.nav.amt.arena.acl.utils.asLocalDate
import no.nav.amt.arena.acl.utils.asLocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class HistDeltakerIntegrationTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val arenaDataRepository: ArenaDataRepository,
	private val deltakerRepository: DeltakerRepository,
	private val arenaDataHistIdTranslationRepository: ArenaDataHistIdTranslationRepository,
	private val arenaDataIdTranslationRepository: ArenaDataIdTranslationRepository,
	private val gjennomforingService: GjennomforingService,
) : IntegrationTestBase() {
	val baseDeltaker = createDeltaker()

	val baseGjennomforing =
		KafkaMessageCreator.baseGjennomforing(
			arenaGjennomforingId = baseDeltaker.TILTAKGJENNOMFORING_ID,
			arbgivIdArrangor = 68968L,
			datoFra = LocalDateTime.now().minusDays(3),
			datoTil = LocalDateTime.now().plusDays(3),
		)

	val gjennomforingIdMR: UUID = UUID.randomUUID()
	val gjennomforingMRData =
		no.nav.amt.arena.acl.clients.mulighetsrommetapi.Gjennomforing(
			id = gjennomforingIdMR,
			tiltakstype =
				no.nav.amt.arena.acl.clients.mulighetsrommetapi.Gjennomforing.Tiltakstype(
					id = UUID.randomUUID(),
					navn = "Navn på tiltak",
					arenaKode = "INDOPPFAG",
				),
			navn = "Navn på gjennomføring",
			startDato = LocalDate.now(),
			sluttDato = LocalDate.now().plusDays(3),
			status = no.nav.amt.arena.acl.clients.mulighetsrommetapi.Gjennomforing.Status.GJENNOMFORES,
			virksomhetsnummer = "999888777",
			oppstart = no.nav.amt.arena.acl.clients.mulighetsrommetapi.Gjennomforing.Oppstartstype.LOPENDE,
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
			gjennomforingIdMR,
		)
		mockMulighetsrommetApiServer.mockHentGjennomforingData(gjennomforingIdMR, gjennomforingMRData)
	}

	@Test
	fun `ingest deltaker - har deltakelse i amt-tiltak, samme gjennomforing og datoer - lagrer id, publiserer ikke deltaker`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		val amtTiltakDeltakerId = UUID.randomUUID()
		val matchingDeltakerId = 31223L
		deltakerRepository.upsert(
			DeltakerInsertDbo(
				arenaId = matchingDeltakerId,
				personId = baseDeltaker.PERSON_ID,
				gjennomforingId = baseDeltaker.TILTAKGJENNOMFORING_ID,
				datoFra = baseDeltaker.DATO_FRA?.asLocalDate(),
				datoTil = baseDeltaker.DATO_TIL?.asLocalDate(),
				regDato = baseDeltaker.REG_DATO.asLocalDateTime(),
				modDato = baseDeltaker.MOD_DATO.asLocalDateTime(),
				status = TiltakDeltaker.Status.GJENN.name,
				datoStatusEndring = baseDeltaker.DATO_STATUSENDRING?.asLocalDateTime(),
				arenaSourceTable = ARENA_DELTAKER_TABLE_NAME,
				eksternId = null,
			),
		)
		arenaDataIdTranslationRepository.insert(
			ArenaDataIdTranslationDbo(
				amtId = amtTiltakDeltakerId,
				arenaTableName = ARENA_DELTAKER_TABLE_NAME,
				arenaId = matchingDeltakerId.toString(),
			),
		)

		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = amtTiltakDeltakerId,
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL),
		)

		baseGjennomforing.publiser {
			it shouldNotBe null
			it!!.isValid shouldBe true
		}

		val pos = "233"
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(baseDeltaker, opPos = pos)),
		)
		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.HANDLED
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldBe null
		}
		arenaDataHistIdTranslationRepository
			.get(
				baseDeltaker.HIST_TILTAKDELTAKER_ID.toString(),
			)?.amtId shouldBe amtTiltakDeltakerId
	}

	@Test
	fun `ingest deltaker - har feilregistrert deltakelse i amt-tiltak, samme gjennomforing og datoer - lagrer id, publiserer deltaker`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		val gjennomforingArenaId = baseDeltaker.TILTAKGJENNOMFORING_ID.toString()
		val amtTiltakDeltakerId = UUID.randomUUID()
		val matchingDeltakerId = 31223L
		deltakerRepository.upsert(
			DeltakerInsertDbo(
				arenaId = matchingDeltakerId,
				personId = baseDeltaker.PERSON_ID,
				gjennomforingId = baseDeltaker.TILTAKGJENNOMFORING_ID,
				datoFra = baseDeltaker.DATO_FRA?.asLocalDate(),
				datoTil = baseDeltaker.DATO_TIL?.asLocalDate(),
				regDato = baseDeltaker.REG_DATO.asLocalDateTime(),
				modDato = baseDeltaker.MOD_DATO.asLocalDateTime(),
				status = TiltakDeltaker.Status.GJENN.name,
				datoStatusEndring = baseDeltaker.DATO_STATUSENDRING?.asLocalDateTime(),
				arenaSourceTable = ARENA_DELTAKER_TABLE_NAME,
				eksternId = null,
			),
		)
		arenaDataIdTranslationRepository.insert(
			ArenaDataIdTranslationDbo(
				amtId = amtTiltakDeltakerId,
				arenaTableName = ARENA_DELTAKER_TABLE_NAME,
				arenaId = matchingDeltakerId.toString(),
			),
		)

		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = amtTiltakDeltakerId,
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL),
			status = DeltakerStatusDto.FEILREGISTRERT,
		)

		baseGjennomforing.publiser {
			it shouldNotBe null
			it!!.isValid shouldBe true
		}

		baseDeltaker.publiserOgValiderOutput {
			it.status shouldBe AmtDeltaker.Status.HAR_SLUTTET
			it.statusAarsak shouldBe null

			val gjennomforing = gjennomforingService.get(gjennomforingArenaId)
			gjennomforing!!.id shouldBe gjennomforingIdMR
		}
		arenaDataHistIdTranslationRepository
			.get(
				baseDeltaker.HIST_TILTAKDELTAKER_ID.toString(),
			)?.amtId shouldBe amtTiltakDeltakerId
	}

	@Test
	fun `ingest deltaker - ingen deltakelser i amt-tiltak - status HANDLED`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = null,
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL),
		)
		val pos = "45"
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(baseDeltaker, opPos = pos)),
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)

			deltakerRecord shouldNotBe null
			arenaData!!.ingestStatus shouldBe IngestStatus.HANDLED
		}
	}

	@Test
	fun `ingest deltaker - har deltakelse i amt-tiltak, annen gjennomforing - status HANDLED`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = UUID.randomUUID(),
			gjennomforingId = UUID.randomUUID(),
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL),
		)

		val pos = "48"
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(baseDeltaker, opPos = pos)),
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)

			deltakerRecord shouldNotBe null
			arenaData!!.ingestStatus shouldBe IngestStatus.HANDLED
		}
	}

	@Test
	fun `ingest deltaker - har deltakelse på samme gjennomforing, andre datoer - status HANDLED`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = UUID.randomUUID(),
			gjennomforingId = gjennomforingIdMR,
			startdato = LocalDate.now().minusDays(1),
			sluttdato = LocalDate.now().plusMonths(3),
		)
		val pos = "52"
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(baseDeltaker, opPos = pos)),
		)

		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)

			deltakerRecord shouldNotBe null
			arenaData!!.ingestStatus shouldBe IngestStatus.HANDLED
		}
	}

	@Test
	fun `ingest deltaker - slettemelding - deltaker blir ikke slettet, melding blir ignored`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = UUID.randomUUID(),
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL),
		)

		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		val pos = "42"
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.TILTAKGJENNOMFORING_ID,
			toJsonString(
				KafkaMessageCreator.opprettArenaHistDeltaker(
					arenaDeltaker = baseDeltaker,
					opPos = pos,
					opType = "D",
				),
			),
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.DELETED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.IGNORED
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldBe null
		}
	}

	@Test
	fun `ingest deltaker - ikke stottet tiltak - skal ignoreres`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = UUID.randomUUID(),
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL),
		)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), "IKKESTOTTET", true)

		val pos = "42"
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(arenaDeltaker = baseDeltaker, opPos = pos)),
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.IGNORED
		}
	}

	@Test
	fun `ingest deltaker - gjennomforing er ugyldig - deltaker far status WAITING`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = UUID.randomUUID(),
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL),
		)
		gjennomforingService.upsert(
			baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(),
			SUPPORTED_TILTAK.first(),
			false,
		)

		val pos = "77"
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(arenaDeltaker = baseDeltaker, opPos = pos)),
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_HIST_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.WAITING
		}
	}

	@Test
	fun `ingest deltaker - uthenting av fnr fra arena feiler - status RETRY`() {
		val pos = "33"
		mockArenaOrdsProxyHttpServer.mockFailHentFnr(baseDeltaker.PERSON_ID!!)
		mockAmtTiltakServer.mockHentDeltakelserForPerson(
			deltakerId = UUID.randomUUID(),
			gjennomforingId = gjennomforingIdMR,
			startdato = parseDate(baseDeltaker.DATO_FRA),
			sluttdato = parseDate(baseDeltaker.DATO_TIL),
		)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
		kafkaMessageSender.publiserArenaHistDeltaker(
			baseDeltaker.HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(baseDeltaker, opPos = pos)),
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

	private fun createDeltaker() =
		KafkaMessageCreator.baseHistDeltaker(
			deltakerStatusKode = TiltakDeltaker.Status.FULLF.name,
			startDato = LocalDate.now().minusMonths(3),
			sluttDato = LocalDate.now().minusMonths(2),
		)

	private fun ArenaGjennomforing.publiser(customAssertions: (payload: Gjennomforing?) -> Unit) {
		kafkaMessageSender.publiserArenaGjennomforing(
			TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(this)),
		)
		AsyncUtils.eventually(until = Duration.ofSeconds(10)) {
			val gjennomforingResult = gjennomforingService.get(TILTAKGJENNOMFORING_ID.toString())
			customAssertions(gjennomforingResult)
		}
	}

	private fun ArenaHistDeltaker.publiserOgValiderOutput(customAssertions: (payload: AmtDeltaker) -> Unit) {
		kafkaMessageSender.publiserArenaHistDeltaker(
			HIST_TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaHistDeltaker(this)),
		)

		AsyncUtils.eventually {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord shouldNotBe null

			val deltakerResult = fromJsonString<AmtKafkaMessageDto<AmtDeltaker>>(deltakerRecord!!.value())
			deltakerResult.type shouldBe PayloadType.DELTAKER
			deltakerResult.operation shouldBe AmtOperation.MODIFIED

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

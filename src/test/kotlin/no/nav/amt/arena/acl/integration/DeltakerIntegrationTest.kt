package no.nav.amt.arena.acl.integration

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.Gjennomforing
import no.nav.amt.arena.acl.domain.db.ArenaDataHistIdTranslationDbo
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
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
import no.nav.amt.arena.acl.repositories.ArenaDataHistIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.services.SUPPORTED_TILTAK
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class DeltakerIntegrationTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaDataIdTranslationRepository: ArenaDataIdTranslationRepository,
	private val arenaDataHistIdTranslationRepository: ArenaDataHistIdTranslationRepository,
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
		no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing(
			id = gjennomforingIdMR,
			tiltakstype =
				no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing.Tiltakstype(
					id = UUID.randomUUID(),
					navn = "Navn på tiltak",
					arenaKode = "INDOPPFAG",
				),
			navn = "Navn på gjennomføring",
			startDato = LocalDate.now(),
			sluttDato = LocalDate.now().plusDays(3),
			status = no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing.Status.GJENNOMFORES,
			virksomhetsnummer = "999888777",
			oppstart = no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing.Oppstartstype.LOPENDE,
		)

	val fnr = "123456789"

	@BeforeEach
	fun setupMocks() {
		mockMulighetsrommetApiServer.reset()
		mockArenaOrdsProxyHttpServer.reset()
		mockMulighetsrommetApiServer.mockHentGjennomforingId(
			baseGjennomforing.TILTAKGJENNOMFORING_ID,
			gjennomforingIdMR,
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

	@Test
	fun `ingest deltaker - forrige melding paa deltaker er ikke ingestet - feiler med status RETRY`() {
		val pos1 = "33"
		val pos2 = "35"

		mockArenaOrdsProxyHttpServer.mockFailHentFnr(baseDeltaker.PERSON_ID!!)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(baseDeltaker, opPos = pos1)),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.CREATED,
					position = pos1,
				)
			arenaData.shouldNotBeNull()
			arenaData.ingestStatus shouldBe IngestStatus.RETRY

			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord.shouldBeNull()
		}

		setupMocks()
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		val endretDeltaker = baseDeltaker.copy(DELTAKERSTATUSKODE = "AKTUELL")
		kafkaMessageSender.publiserArenaDeltaker(
			endretDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(endretDeltaker, opPos = pos2)),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.CREATED,
					position = pos2,
				)

			assertSoftly(arenaData.shouldNotBeNull()) {
				ingestStatus shouldBe IngestStatus.RETRY
				note shouldBe "Forrige melding på deltaker med id=${baseDeltaker.TILTAKDELTAKER_ID} er ikke håndtert enda"
			}

			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord.shouldBeNull()
		}
	}

	@Test
	fun `ingest deltaker - ikke støttet tiltak - skal ignoreres`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), "IKKESTOTTET", true)

		val pos = "42"
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = baseDeltaker, opPos = pos)),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.CREATED,
					position = pos,
				)
			arenaData.shouldNotBeNull()
			arenaData.ingestStatus shouldBe IngestStatus.IGNORED
		}
	}

	@Test
	fun `ingest deltaker - gjennomføring er ugyldig - deltaker får status WAITING`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingService.upsert(
			baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(),
			SUPPORTED_TILTAK.first(),
			false,
		)

		val pos = "77"
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = baseDeltaker, opPos = pos)),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.CREATED,
					position = pos,
				)
			arenaData.shouldNotBeNull()
			arenaData.ingestStatus shouldBe IngestStatus.WAITING
		}
	}

	@Test
	fun `ingest deltaker - deltaker ingestet med andre data - oppdaterer deltaker`() {
		val endretDeltaker =
			KafkaMessageCreator.baseDeltaker(
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
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(baseDeltaker, opPos = pos)),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.CREATED,
					position = pos,
				)
			arenaData.shouldNotBeNull()
			arenaData.ingestStatus shouldBe IngestStatus.RETRY

			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(topic = KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord.shouldBeNull()
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
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(deltaker, opPos = pos)),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.CREATED,
					position = pos,
				)
			arenaData.shouldNotBeNull()
			arenaData.ingestStatus shouldBe IngestStatus.INVALID

			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(topic = KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord.shouldBeNull()
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
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(deltaker, opPos = pos)),
		)

		deltaker.publiserOgValiderOutput {
			it.dagerPerUke shouldBe 2.5f
		}
	}

	@Test
	fun `ingest deltaker - siste melding for samme deltaker var behandlet for mindre enn 500ms siden - venter litt før den behandles`() {
		val deltaker = baseDeltaker.copy(TILTAKDELTAKER_ID = 0)
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		val ingestedTimestamp = LocalDateTime.now()

		arenaDataRepository.upsert(
			ArenaDataUpsertInput(
				arenaTableName = ARENA_DELTAKER_TABLE_NAME,
				arenaId = deltaker.TILTAKDELTAKER_ID.toString(),
				operation = AmtOperation.CREATED,
				operationPosition = "66",
				operationTimestamp = LocalDateTime.now().minusDays(1),
				ingestStatus = IngestStatus.HANDLED,
				ingestedTimestamp = ingestedTimestamp,
			),
		)

		kafkaMessageSender.publiserArenaDeltaker(
			deltaker.TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(deltaker, opPos = "67", opType = "U")),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.MODIFIED,
					position = "67",
				)
			arenaData.shouldNotBeNull()
			arenaData.ingestedTimestamp!! shouldBeAfter
				ingestedTimestamp
					.plus(Duration.ofMillis(500))
		}
	}

	@Test
	fun `slett deltaker - hist-deltaker finnes - deltaker blir ikke slettet, melding blir handled`() {
		val amtId = UUID.randomUUID()
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)
		arenaDataIdTranslationRepository.insert(
			ArenaDataIdTranslationDbo(amtId, ARENA_DELTAKER_TABLE_NAME, baseDeltaker.TILTAKDELTAKER_ID.toString()),
		)
		arenaDataHistIdTranslationRepository.insert(
			ArenaDataHistIdTranslationDbo(
				amtId,
				"123",
				baseDeltaker.TILTAKDELTAKER_ID.toString(),
			),
		)

		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		val pos = "42"
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(
				KafkaMessageCreator.opprettArenaDeltaker(
					arenaDeltaker = baseDeltaker,
					opPos = pos,
					opType = "D",
				),
			),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.DELETED,
					position = pos,
				)

			assertSoftly(arenaData.shouldNotBeNull()) {
				note shouldBe "Slettes ikke fordi deltaker ble historisert"
				ingestStatus shouldBe IngestStatus.HANDLED
			}

			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord.shouldBeNull()
		}
	}

	@Test
	fun `slett deltaker - hist-deltaker finnes ikke - melding blir retry`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)

		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		val pos = "443"
		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(
				KafkaMessageCreator.opprettArenaDeltaker(
					arenaDeltaker = baseDeltaker,
					opPos = pos,
					opType = "D",
				),
			),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.DELETED,
					position = pos,
				)

			arenaData.shouldNotBeNull()
			arenaData.ingestStatus shouldBe IngestStatus.RETRY

			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord.shouldBeNull()
		}
	}

	@Test
	fun `slett deltaker - hist-deltaker finnes ikke, retryet tre ganger - deltaker blir feilregistrert, melding blir handled`() {
		mockArenaOrdsProxyHttpServer.mockHentFnr(baseDeltaker.PERSON_ID!!, fnr)

		gjennomforingService.upsert(baseGjennomforing.TILTAKGJENNOMFORING_ID.toString(), SUPPORTED_TILTAK.first(), true)

		val pos = "73"
		arenaDataRepository.upsert(
			ArenaDataUpsertInput(
				arenaTableName = ARENA_DELTAKER_TABLE_NAME,
				arenaId = baseDeltaker.TILTAKDELTAKER_ID.toString(),
				operation = AmtOperation.DELETED,
				operationPosition = pos,
				operationTimestamp = LocalDateTime.now().minusDays(1),
				ingestStatus = IngestStatus.RETRY,
				ingestedTimestamp = null,
			),
		)
		val arenaDataId =
			arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.DELETED, pos)?.id
				?: throw RuntimeException()
		arenaDataRepository.updateIngestAttempts(arenaDataId, 3, null)

		kafkaMessageSender.publiserArenaDeltaker(
			baseDeltaker.TILTAKDELTAKER_ID,
			toJsonString(
				KafkaMessageCreator.opprettArenaDeltaker(
					arenaDeltaker = baseDeltaker,
					opPos = pos,
					opType = "D",
				),
			),
		)

		await().untilAsserted {
			val arenaData =
				arenaDataRepository.get(
					tableName = ARENA_DELTAKER_TABLE_NAME,
					operation = AmtOperation.DELETED,
					position = pos,
				)

			arenaData.shouldNotBeNull()
			arenaData.ingestStatus shouldBe IngestStatus.HANDLED

			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord.shouldNotBeNull()

			val deltakerResult = fromJsonString<AmtKafkaMessageDto<AmtDeltaker>>(deltakerRecord.value())
			assertSoftly(deltakerResult) {
				type shouldBe PayloadType.DELTAKER
				operation shouldBe AmtOperation.MODIFIED
			}

			assertSoftly(deltakerResult.payload.shouldNotBeNull()) {
				personIdent shouldBe fnr
				gjennomforingId shouldBe gjennomforingIdMR
				status shouldBe AmtDeltaker.Status.FEILREGISTRERT
				innsokBegrunnelse shouldBe null
			}
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

	private fun createDeltaker() =
		KafkaMessageCreator.baseDeltaker(
			startDato = LocalDate.now().plusDays(1),
			sluttDato = LocalDate.now().plusMonths(6),
		)

	fun ArenaGjennomforing.publiser(customAssertions: (payload: Gjennomforing?) -> Unit) {
		kafkaMessageSender.publiserArenaGjennomforing(
			TILTAKGJENNOMFORING_ID,
			toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(this)),
		)

		await().untilAsserted {
			val gjennomforingResult = gjennomforingService.get(TILTAKGJENNOMFORING_ID.toString())
			customAssertions(gjennomforingResult)
		}
	}

	fun ArenaDeltaker.publiserOgValiderOutput(customAssertions: (payload: AmtDeltaker) -> Unit) {
		kafkaMessageSender.publiserArenaDeltaker(
			TILTAKDELTAKER_ID,
			toJsonString(KafkaMessageCreator.opprettArenaDeltaker(this)),
		)

		await().untilAsserted {
			val deltakerRecord = kafkaMessageConsumer.getLatestRecord(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord.shouldNotBeNull()

			val deltakerResult = fromJsonString<AmtKafkaMessageDto<AmtDeltaker>>(deltakerRecord.value())
			assertSoftly(deltakerResult) {
				type shouldBe PayloadType.DELTAKER
				operation shouldBe AmtOperation.CREATED

				assertSoftly(payload.shouldNotBeNull()) {
					personIdent shouldBe fnr
					gjennomforingId shouldBe gjennomforingIdMR
					customAssertions(it)
					validate(this@publiserOgValiderOutput)
				}
			}
		}
	}
}

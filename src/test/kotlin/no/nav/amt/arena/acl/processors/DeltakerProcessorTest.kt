package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.metrics.DeltakerMetricHandler
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClient
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

class DeltakerProcessorTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	val ordsClient = mock<ArenaOrdsProxyClient> {
		on { hentFnr(anyString()) } doReturn "01010051234"
	}

	@SuppressWarnings("unchecked")
	val kafkaProducer = mock<KafkaProducerClient<String, String>> {
		on { sendSync(any()) } doReturn null
	}

	lateinit var arenaDataRepository: ArenaDataRepository
	lateinit var idTranslationRepository: ArenaDataIdTranslationRepository

	lateinit var deltakerProcessor: DeltakerProcessor

	val nonIgnoredGjennomforingArenaId = 1L
	val ignoredGjennomforingArenaId = 2L

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		val template = NamedParameterJdbcTemplate(dataSource)
		arenaDataRepository = ArenaDataRepository(template)
		idTranslationRepository = ArenaDataIdTranslationRepository(template)

		DatabaseTestUtils.cleanAndInitDatabase(dataSource, "/deltaker-processor_test-data.sql")

		deltakerProcessor = DeltakerProcessor(
			arenaDataRepository = arenaDataRepository,
			arenaDataIdTranslationService = ArenaDataIdTranslationService(idTranslationRepository),
			ordsClient = ordsClient,
			meterRegistry = SimpleMeterRegistry(),
			kafkaProducerService = KafkaProducerService(kafkaProducer),
			metrics = DeltakerMetricHandler(SimpleMeterRegistry())
		)

		ReflectionTestUtils.setField(deltakerProcessor, "topic", "test-topic")

	}

	fun getAndCheckArenaDataRepositoryEntry(
		operation: AmtOperation,
		position: String,
		expectedStatus: IngestStatus = IngestStatus.HANDLED
	): ArenaDataDbo {
		val arenaDataRepositoryEntry = shouldNotThrowAny {
			arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, operation, position)
		}

		arenaDataRepositoryEntry shouldNotBe null
		arenaDataRepositoryEntry.ingestStatus shouldBe expectedStatus

		when (arenaDataRepositoryEntry.operation) {
			AmtOperation.CREATED -> {
				arenaDataRepositoryEntry.before shouldBe null
				arenaDataRepositoryEntry.after shouldNotBe null
			}
			AmtOperation.MODIFIED -> {
				arenaDataRepositoryEntry.before shouldNotBe null
				arenaDataRepositoryEntry.after shouldNotBe null
			}
			AmtOperation.DELETED -> {
				arenaDataRepositoryEntry.before shouldNotBe null
				arenaDataRepositoryEntry.after shouldBe null
			}
		}

		return arenaDataRepositoryEntry
	}


	test("Insert Deltaker on non-ignored Gjennomforing") {
		val position = UUID.randomUUID().toString()

		val newDeltaker = createNewDeltakerArenaData(
			position = position,
			tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
			deltakerArenaId = 1L
		)

		deltakerProcessor.handle(newDeltaker)

		getAndCheckArenaDataRepositoryEntry(operation = AmtOperation.CREATED, position)

		val translationEntry = idTranslationRepository.get(ARENA_DELTAKER_TABLE_NAME, "1")

		translationEntry shouldNotBe null
		translationEntry!!.ignored shouldBe false
	}

	test("Insert Deltaker with gjennomføring not processed set the Deltaker to retry") {
		val position = UUID.randomUUID().toString()

		deltakerProcessor.handle(
			createNewDeltakerArenaData(
				position,
				2348790L,
				1L
			)
		)

		val arenaDataEntry = getAndCheckArenaDataRepositoryEntry(AmtOperation.CREATED, position, IngestStatus.RETRY)
		arenaDataEntry.ingestAttempts shouldBe 1
		arenaDataEntry.lastAttempted shouldNotBe null
	}

	test("Insert Deltaker on Ignored Gjennomføring sets Deltaker to Ingored") {
		val position = UUID.randomUUID().toString()

		deltakerProcessor.handle(
			createNewDeltakerArenaData(
				position,
				ignoredGjennomforingArenaId,
				1L
			)
		)

		getAndCheckArenaDataRepositoryEntry(AmtOperation.CREATED, position, IngestStatus.IGNORED)
	}

	test("Should process deleted deltaker") {
		val position = UUID.randomUUID().toString()

		deltakerProcessor.handle(
			createNewDeltakerArenaData(
				position = position,
				tiltakGjennomforingArenaId = nonIgnoredGjennomforingArenaId,
				deltakerArenaId = 1L,
				operation = AmtOperation.DELETED
			)
		)

		getAndCheckArenaDataRepositoryEntry(AmtOperation.DELETED, position, IngestStatus.HANDLED)
	}

})



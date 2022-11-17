package no.nav.amt.arena.acl.services

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.processors.DeltakerProcessor
import no.nav.amt.arena.acl.processors.GjennomforingProcessor
import no.nav.amt.arena.acl.processors.SakProcessor
import no.nav.amt.arena.acl.processors.TiltakProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ArenaMessageProcessorServiceTest {

	val mapper = ObjectMapperFactory.get()

	lateinit var arenaDataRepository: ArenaDataRepository

	lateinit var tiltakProcessor: TiltakProcessor

	lateinit var gjennomforingProcessor: GjennomforingProcessor

	lateinit var deltakerProcessor: DeltakerProcessor

	lateinit var sakProcessor: SakProcessor

	lateinit var meterRegistry: MeterRegistry

	lateinit var messageProcessor: ArenaMessageProcessorService

	@BeforeEach
	internal fun setUp() {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		arenaDataRepository = mockk()
		tiltakProcessor = mockk()
		gjennomforingProcessor = mockk()
		sakProcessor = mockk()
		deltakerProcessor = mockk()

		meterRegistry = SimpleMeterRegistry()

		messageProcessor = ArenaMessageProcessorService(
			tiltakProcessor = tiltakProcessor,
			gjennomforingProcessor = gjennomforingProcessor,
			deltakerProcessor = deltakerProcessor,
			sakProcessor = sakProcessor,
			arenaDataRepository = arenaDataRepository,
			meterRegistry = meterRegistry
		)
	}

	@Test
	internal fun `Should handle arena deltaker message`() {
		defaultMocks()

		val tiltakdeltakereJsonFileContent =
			javaClass.classLoader.getResource("data/arena-tiltakdeltakerendret-v1.json").readText()
		val tiltakdeltakere: List<JsonNode> = mapper.readValue(tiltakdeltakereJsonFileContent)
		val deltakerJson = tiltakdeltakere.toList()[0].toString()

		every {
			deltakerProcessor.handleArenaMessage(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", deltakerJson)
		)

		verify(exactly = 1) {
			deltakerProcessor.handleArenaMessage(any())
		}
	}

	@Test
	internal fun `Should handle arena gjennomforing message`() {
		defaultMocks()

		val tiltakgjennomforingerJsonFileContent =
			javaClass.classLoader.getResource("data/arena-tiltakgjennomforingendret-v1.json").readText()
		val tiltakgjennomforinger: List<JsonNode> = mapper.readValue(tiltakgjennomforingerJsonFileContent)
		val tiltakgjennomforingJson = tiltakgjennomforinger.toList()[0].toString()

		every {
			gjennomforingProcessor.handleArenaMessage(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", tiltakgjennomforingJson)
		)

		verify(exactly = 1) {
			gjennomforingProcessor.handleArenaMessage(any())
		}

	}

	@Test
	internal fun `Should handle arena tiltak message`() {
		defaultMocks()

		val tiltakJsonFileContent =
			javaClass.classLoader.getResource("data/arena-tiltakendret-v1.json").readText()
		val tiltakList: List<JsonNode> = mapper.readValue(tiltakJsonFileContent)
		val tiltakJson = tiltakList.toList()[0].toString()

		every {
			tiltakProcessor.handleArenaMessage(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", tiltakJson)
		)

		verify(exactly = 1) {
			tiltakProcessor.handleArenaMessage(any())
		}
	}

	@Test
	internal fun `Should handle message with unicode NULL`() {
		defaultMocks()

		val tiltakgjennomforingerJsonFileContent =
			javaClass.classLoader.getResource("data/arena-tiltakgjennomforingendret-v1-bad-unicode.json")
				.readText()
		val tiltakgjennomforinger: List<JsonNode> = mapper.readValue(tiltakgjennomforingerJsonFileContent)
		val tiltakgjennomforingJson = tiltakgjennomforinger.toList()[0].toString()

		every {
			gjennomforingProcessor.handleArenaMessage(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", tiltakgjennomforingJson)
		)

		val capturingSlot = CapturingSlot<ArenaGjennomforingKafkaMessage>()

		verify(exactly = 1) {
			gjennomforingProcessor.handleArenaMessage(capture(capturingSlot))
		}

		val capturedData = capturingSlot.captured

		capturedData.after?.VURDERING_GJENNOMFORING shouldBe "Vurdering"

	}

	@Test
	internal fun `Should handle arena sak message`() {
		defaultMocks()

		val tiltakJsonFileContent =
			javaClass.classLoader.getResource("data/arena-sakendret-v1.json").readText()
		val sakList: List<JsonNode> = mapper.readValue(tiltakJsonFileContent)
		val sakJson = sakList.toList()[0].toString()

		every {
			sakProcessor.handleArenaMessage(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", sakJson)
		)

		verify(exactly = 1) {
			sakProcessor.handleArenaMessage(any())
		}

	}
	@Test
	internal fun `Set older unhandled messages to NEWER_MESSAGE_RECEIVED`() {
		val messages = listOf(
			arenaDataDbo(id = 1, ingestStatus = IngestStatus.RETRY),
			arenaDataDbo(id = 2, ingestStatus = IngestStatus.NEW),
			arenaDataDbo(id = 3, ingestStatus = IngestStatus.FAILED),
			arenaDataDbo(id = 4, ingestStatus = IngestStatus.IGNORED),
			arenaDataDbo(id = 5, ingestStatus = IngestStatus.HANDLED),
			arenaDataDbo(id = 6, ingestStatus = IngestStatus.INVALID),
		)

		every { arenaDataRepository.getByArenaId(any(), any()) } returns messages
		every { arenaDataRepository.updateIngestStatus(ids = any(), ingestStatus = any()) } returns Unit

		messageProcessor.setOlderUnhandledMessagesToNewerMessageReceived("Table", "ARENA_ID", LocalDateTime.now())

		val capturingSlot = CapturingSlot<Set<Int>>()

		verify (exactly = 1){
			arenaDataRepository.updateIngestStatus(ids = capture(capturingSlot), ingestStatus = any())
		}

		capturingSlot.captured shouldContainExactly setOf(1, 2, 3)
	}

	private fun defaultMocks() {
		every { arenaDataRepository.getByArenaId(any(), any()) } returns listOf()
		every { arenaDataRepository.updateIngestStatus(ids = any(), ingestStatus = any()) } returns Unit
	}

	private fun arenaDataDbo(
		id: Int = 0,
		ingestStatus: IngestStatus = IngestStatus.NEW
	): ArenaDataDbo = ArenaDataDbo(
		id = id,
		arenaTableName = "Table",
		arenaId = "ARENA_ID",
		operation = AmtOperation.CREATED,
		operationPosition = "0",
		operationTimestamp = LocalDateTime.now(),
		ingestStatus = ingestStatus,
		ingestedTimestamp = null,
		ingestAttempts = 0,
		lastAttempted = null,
		before = null,
		after = null,
		note = null
	)

}

package no.nav.amt.arena.acl.services

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.processors.DeltakerProcessor
import no.nav.amt.arena.acl.processors.GjennomforingProcessor
import no.nav.amt.arena.acl.processors.SakProcessor
import no.nav.amt.arena.acl.processors.TiltakProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class ArenaMessageProcessorServiceTest : StringSpec({

	val mapper = ObjectMapperFactory.get()

	lateinit var arenaDataRepository: ArenaDataRepository

	lateinit var tiltakProcessor: TiltakProcessor

	lateinit var gjennomforingProcessor: GjennomforingProcessor

	lateinit var deltakerProcessor: DeltakerProcessor

	lateinit var sakProcessor: SakProcessor

	lateinit var meterRegistry: MeterRegistry

	lateinit var messageProcessor: ArenaMessageProcessorService

	beforeEach {
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

	"should handle arena deltaker message" {
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

	"should handle arena gjennomforing message" {
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

	"should handle arena tiltak message" {
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

	"should handle message with unicode NULL" {
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

	"should handle arena sak message" {
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

})

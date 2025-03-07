package no.nav.amt.arena.acl.services

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.databind.JsonNode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.consumer.ArenaDeltakerConsumer
import no.nav.amt.arena.acl.consumer.GjennomforingConsumer
import no.nav.amt.arena.acl.consumer.HistDeltakerConsumer
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class ArenaMessageConsumerServiceTest : StringSpec({

	lateinit var arenaDataRepository: ArenaDataRepository

	lateinit var gjennomforingConsumer: GjennomforingConsumer

	lateinit var arenaDeltakerConsumer: ArenaDeltakerConsumer

	lateinit var histDeltakerProcessor: HistDeltakerConsumer

	lateinit var meterRegistry: MeterRegistry

	lateinit var messageProcessor: ArenaMessageConsumerService

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		arenaDataRepository = mockk()
		gjennomforingConsumer = mockk()
		arenaDeltakerConsumer = mockk()
		histDeltakerProcessor = mockk()

		meterRegistry = SimpleMeterRegistry()

		messageProcessor = ArenaMessageConsumerService(
			gjennomforingConsumer = gjennomforingConsumer,
			arenaDeltakerConsumer = arenaDeltakerConsumer,
			histDeltakerConsumer = histDeltakerProcessor,
			arenaDataRepository = arenaDataRepository,
			meterRegistry = meterRegistry
		)
	}

	"should handle arena deltaker message" {
		val tiltakdeltakereJsonFileContent =
			javaClass.classLoader.getResource("data/arena-tiltakdeltakerendret-v1.json").readText()
		val tiltakdeltakere: List<JsonNode> = fromJsonString(tiltakdeltakereJsonFileContent)
		val deltakerJson = tiltakdeltakere.toList()[0].toString()

		every {
			arenaDeltakerConsumer.handleArenaMessage(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", deltakerJson)
		)

		verify(exactly = 1) {
			arenaDeltakerConsumer.handleArenaMessage(any())
		}
	}

	"should handle arena hist deltaker message" {
		val histTiltakdeltakereJsonFileContent =
			javaClass.classLoader.getResource("data/arena-histtiltakdeltakerendret-v1.json").readText()
		val histTiltakdeltakere: List<JsonNode> = fromJsonString(histTiltakdeltakereJsonFileContent)
		val histDeltakerJson = histTiltakdeltakere.toList()[0].toString()

		every {
			histDeltakerProcessor.handleArenaMessage(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", histDeltakerJson)
		)

		verify(exactly = 1) {
			histDeltakerProcessor.handleArenaMessage(any())
		}
	}

	"should handle arena gjennomforing message" {
		val tiltakgjennomforingerJsonFileContent =
			javaClass.classLoader.getResource("data/arena-tiltakgjennomforingendret-v1.json").readText()
		val tiltakgjennomforinger: List<JsonNode> = fromJsonString(tiltakgjennomforingerJsonFileContent)
		val tiltakgjennomforingJson = tiltakgjennomforinger.toList()[0].toString()

		every {
			gjennomforingConsumer.handleArenaMessage(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", tiltakgjennomforingJson)
		)

		verify(exactly = 1) {
			gjennomforingConsumer.handleArenaMessage(any())
		}
	}


	"should handle message with unicode NULL" {
		val tiltakgjennomforingerJsonFileContent =
			javaClass.classLoader.getResource("data/arena-tiltakgjennomforingendret-v1-bad-unicode.json")
				.readText()
		val tiltakgjennomforinger: List<JsonNode> = fromJsonString(tiltakgjennomforingerJsonFileContent)
		val tiltakgjennomforingJson = tiltakgjennomforinger.toList()[0].toString()

		every {
			gjennomforingConsumer.handleArenaMessage(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", tiltakgjennomforingJson)
		)

		val capturingSlot = CapturingSlot<ArenaGjennomforingKafkaMessage>()

		verify(exactly = 1) {
			gjennomforingConsumer.handleArenaMessage(capture(capturingSlot))
		}

		val capturedData = capturingSlot.captured

		capturedData.after?.VURDERING_GJENNOMFORING shouldBe "Vurdering"
	}

})

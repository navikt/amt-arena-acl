package no.nav.amt.arena.acl.services

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.arena.acl.consumer.ArenaDeltakerConsumer
import no.nav.amt.arena.acl.consumer.GjennomforingConsumer
import no.nav.amt.arena.acl.consumer.HistDeltakerConsumer
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ClassPathResourceUtils.readResourceAsText
import no.nav.amt.arena.acl.utils.JsonUtils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.readValue

class ArenaMessageConsumerServiceTest :
	StringSpec({
		val arenaDataRepository: ArenaDataRepository = mockk()
		val gjennomforingConsumer: GjennomforingConsumer = mockk()
		val arenaDeltakerConsumer: ArenaDeltakerConsumer = mockk()
		val histDeltakerProcessor: HistDeltakerConsumer = mockk()

		val messageProcessor =
			ArenaMessageConsumerService(
				gjennomforingConsumer = gjennomforingConsumer,
				arenaDeltakerConsumer = arenaDeltakerConsumer,
				histDeltakerConsumer = histDeltakerProcessor,
				arenaDataRepository = arenaDataRepository,
				meterRegistry = SimpleMeterRegistry(),
			)

		beforeEach {
			clearAllMocks()
			every { gjennomforingConsumer.handleArenaMessage(any()) } just Runs
			every { arenaDeltakerConsumer.handleArenaMessage(any()) } just Runs
			every { histDeltakerProcessor.handleArenaMessage(any()) } just Runs

			val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
			rootLogger.level = Level.WARN
		}

		"should handle arena deltaker message" {
			val tiltakdeltakereJsonFileContent = readResourceAsText("data/arena-tiltakdeltakerendret-v1.json")
			val tiltakdeltakere: List<JsonNode> = objectMapper.readValue(tiltakdeltakereJsonFileContent)
			val deltakerJson = tiltakdeltakere.toList()[0].toString()

			messageProcessor.handleArenaGoldenGateRecord(
				ConsumerRecord("test", 1, 1, "123456", deltakerJson),
			)

			verify(exactly = 1) {
				arenaDeltakerConsumer.handleArenaMessage(any())
			}
		}

		"should handle arena hist deltaker message" {
			val histTiltakdeltakereJsonFileContent = readResourceAsText("data/arena-histtiltakdeltakerendret-v1.json")
			val histTiltakdeltakere: List<JsonNode> = objectMapper.readValue(histTiltakdeltakereJsonFileContent)
			val histDeltakerJson = histTiltakdeltakere.toList()[0].toString()

			messageProcessor.handleArenaGoldenGateRecord(
				ConsumerRecord("test", 1, 1, "123456", histDeltakerJson),
			)

			verify(exactly = 1) {
				histDeltakerProcessor.handleArenaMessage(any())
			}
		}

		"should handle arena gjennomforing message" {
			val tiltakgjennomforingerJsonFileContent = readResourceAsText("data/arena-tiltakgjennomforingendret-v1.json")
			val tiltakgjennomforinger: List<JsonNode> = objectMapper.readValue(tiltakgjennomforingerJsonFileContent)
			val tiltakgjennomforingJson = tiltakgjennomforinger.toList()[0].toString()

			messageProcessor.handleArenaGoldenGateRecord(
				ConsumerRecord("test", 1, 1, "123456", tiltakgjennomforingJson),
			)

			verify(exactly = 1) {
				gjennomforingConsumer.handleArenaMessage(any())
			}
		}

		"should handle message with unicode NULL" {
			val tiltakgjennomforingerJsonFileContent =
				readResourceAsText("data/arena-tiltakgjennomforingendret-v1-bad-unicode.json")
			val tiltakgjennomforinger: List<JsonNode> = objectMapper.readValue(tiltakgjennomforingerJsonFileContent)
			val tiltakgjennomforingJson = tiltakgjennomforinger.toList()[0].toString()

			messageProcessor.handleArenaGoldenGateRecord(
				ConsumerRecord("test", 1, 1, "123456", tiltakgjennomforingJson),
			)

			val capturingSlot = CapturingSlot<ArenaGjennomforingKafkaMessage>()

			verify(exactly = 1) {
				gjennomforingConsumer.handleArenaMessage(capture(capturingSlot))
			}

			val capturedData = capturingSlot.captured

			capturedData.after?.VURDERING_GJENNOMFORING shouldBe "Vurdering"
		}
	})

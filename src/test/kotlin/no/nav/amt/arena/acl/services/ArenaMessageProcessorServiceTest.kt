package no.nav.amt.arena.acl.services

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.processors.DeltakerProcessor
import no.nav.amt.arena.acl.processors.TiltakGjennomforingProcessor
import no.nav.amt.arena.acl.processors.TiltakProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class ArenaMessageProcessorServiceTest : StringSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	val objectMapper = jacksonObjectMapper()

	lateinit var arenaDataRepository: ArenaDataRepository

	lateinit var tiltakProcessor: TiltakProcessor

	lateinit var tiltakGjennomforingProcessor: TiltakGjennomforingProcessor

	lateinit var deltakerProcessor: DeltakerProcessor

	lateinit var messageProcessor: ArenaMessageProcessorService

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		arenaDataRepository = ArenaDataRepository(NamedParameterJdbcTemplate(dataSource))

		tiltakProcessor = mockk()
		tiltakGjennomforingProcessor = mockk()
		deltakerProcessor = mockk()

		messageProcessor = ArenaMessageProcessorService(
			arenaDataRepository,
			tiltakProcessor,
			tiltakGjennomforingProcessor,
			deltakerProcessor
		)

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	"should handle arena deltaker message" {
		val tiltakdeltakereJsonFileContent = this::class.java.classLoader.getResource("data/arena-tiltakdeltakerendret-v1.json").readText()
		val tiltakdeltakere: List<JsonNode> = objectMapper.readValue(tiltakdeltakereJsonFileContent)
		val deltakerJson = tiltakdeltakere.toList()[0].toString()

		every {
			deltakerProcessor.handle(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", deltakerJson)
		)

		verify(exactly = 1) {
			deltakerProcessor.handle(any())
		}
	}

	"should handle arena gjennomforing message" {
		val tiltakgjennomforingerJsonFileContent = this::class.java.classLoader.getResource("data/arena-tiltakgjennomforingendret-v1.json").readText()
		val tiltakgjennomforinger: List<JsonNode> = objectMapper.readValue(tiltakgjennomforingerJsonFileContent)
		val tiltakgjennomforingJson = tiltakgjennomforinger.toList()[0].toString()

		every {
			tiltakGjennomforingProcessor.handle(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", tiltakgjennomforingJson)
		)

		verify(exactly = 1) {
			tiltakGjennomforingProcessor.handle(any())
		}
	}

	"should handle arena tiltak message" {
		val tiltakJsonFileContent = this::class.java.classLoader.getResource("data/arena-tiltakendret-v1.json").readText()
		val tiltakList: List<JsonNode> = objectMapper.readValue(tiltakJsonFileContent)
		val tiltakJson = tiltakList.toList()[0].toString()

		every {
			tiltakProcessor.handle(any())
		} returns Unit

		messageProcessor.handleArenaGoldenGateRecord(
			ConsumerRecord("test", 1, 1, "123456", tiltakJson)
		)

		verify(exactly = 1) {
			tiltakProcessor.handle(any())
		}
	}

})

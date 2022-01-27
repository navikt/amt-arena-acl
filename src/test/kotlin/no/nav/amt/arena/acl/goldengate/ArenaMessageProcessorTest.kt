package no.nav.amt.arena.acl.goldengate

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

class ArenaMessageProcessorTest : StringSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	val objectMapper = jacksonObjectMapper()

	val tiltakdeltakereJsonFileContent = this::class.java.classLoader.getResource("data/arena-tiltakdeltakerendret-v1.json").readText()

	val tiltakdeltakere: List<JsonNode> = objectMapper.readValue(tiltakdeltakereJsonFileContent)

	lateinit var arenaDataRepository: ArenaDataRepository

	lateinit var arenaDataIdTranslationRepository: ArenaDataIdTranslationRepository

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		arenaDataRepository = ArenaDataRepository(NamedParameterJdbcTemplate(dataSource))
		arenaDataIdTranslationRepository = ArenaDataIdTranslationRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	"should store arena data that is relevant" {
		val processor = ArenaMessageProcessor(
			arenaDataRepository,
			arenaDataIdTranslationRepository
		)

		arenaDataIdTranslationRepository.insert(ArenaDataIdTranslation(
			UUID.randomUUID(),
			"SIAMO.TILTAKDELTAKER",
			"123456",
			false,
			""
		))

		processor.handleArenaGoldenGateRecord(ConsumerRecord("test", 1, 1, "123456", tiltakdeltakere.toList()[0].toString()))

		arenaDataRepository.getAll().size shouldBe 1
	}

	"should store arena data when unsure if relevant" {
		val processor = ArenaMessageProcessor(
			arenaDataRepository,
			arenaDataIdTranslationRepository
		)

		processor.handleArenaGoldenGateRecord(ConsumerRecord("test", 1, 1, "123456", tiltakdeltakere.toList()[0].toString()))

		arenaDataRepository.getAll().size shouldBe 1
	}

	"should not store arena data that is irrelevant" {
		val processor = ArenaMessageProcessor(
			arenaDataRepository,
			arenaDataIdTranslationRepository
		)

		arenaDataIdTranslationRepository.insert(ArenaDataIdTranslation(
			UUID.randomUUID(),
			"SIAMO.TILTAKDELTAKER",
			"123456",
			true,
			""
		))

		processor.handleArenaGoldenGateRecord(ConsumerRecord("test", 1, 1, "123456", tiltakdeltakere.toList()[0].toString()))

		arenaDataRepository.getAll().size shouldBe 0
	}

})

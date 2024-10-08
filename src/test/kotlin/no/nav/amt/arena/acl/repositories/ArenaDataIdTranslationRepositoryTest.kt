package no.nav.amt.arena.acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class ArenaDataIdTranslationRepositoryTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var repository: ArenaDataIdTranslationRepository

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		repository = ArenaDataIdTranslationRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("get(arenaId) - skal returnere eksisterende record") {
		val testObject = ArenaDataIdTranslationDbo(
			amtId = UUID.randomUUID(),
			arenaTableName = "ARENA_TABLE_NAME",
			arenaId = "ARENA_ID"
		)

		repository.insert(testObject)

		val stored = repository.get(testObject.arenaTableName, testObject.arenaId)

		stored shouldNotBe null
		stored!!.amtId shouldBe testObject.amtId
		stored.arenaTableName shouldBe testObject.arenaTableName
		stored.arenaId shouldBe testObject.arenaId
	}

	test("get(amtId) - skal returnere arenaId") {
		val testObject = ArenaDataIdTranslationDbo(
			amtId = UUID.randomUUID(),
			arenaTableName = "ARENA_TABLE_NAME",
			arenaId = "ARENA_ID123"
		)
		repository.insert(testObject)

		val stored = repository.get(testObject.amtId)

		stored shouldNotBe null
		stored!!.arenaTableName shouldBe testObject.arenaTableName
		stored.arenaId shouldBe testObject.arenaId
	}
})

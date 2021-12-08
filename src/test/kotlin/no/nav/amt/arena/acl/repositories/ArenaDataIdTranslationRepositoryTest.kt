package no.nav.amt.arena.acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

class ArenaDataIdTranslationRepositoryTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var repository: ArenaDataIdTranslationRepository

	val testObject = ArenaDataIdTranslation(
		amtId = UUID.randomUUID(),
		arenaTableName = "ARENA_TABLE_NAME",
		arenaId = "ARENA_ID",
		ignored = true
	)

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		repository = ArenaDataIdTranslationRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("Insert and get should return inserted object") {
		repository.insert(testObject)

		val stored = repository.get(testObject.arenaTableName, testObject.arenaId)

		stored shouldNotBe null
		stored!!.amtId shouldBe testObject.amtId
		stored.arenaTableName shouldBe testObject.arenaTableName
		stored.arenaId shouldBe testObject.arenaId
		stored.ignored shouldBe testObject.ignored
	}

	test("Insert twice on same table + arena_id should throw exception") {
		repository.insert(testObject)

		val exception = shouldThrowExactly<IllegalStateException> { repository.insert(testObject) }

		exception.message shouldBe "Translation entry on table ${testObject.arenaTableName} with id ${testObject.arenaId} already exist."
	}

})

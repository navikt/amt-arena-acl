package no.nav.amt.arena.acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@SpringBootTest(classes = [ArenaDataIdTranslationRepository::class])
class ArenaDataIdTranslationRepositoryTest(
	private val dataIdTranslationRepository: ArenaDataIdTranslationRepository,
) : KotestRepositoryTestBase({

		beforeEach {
			val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
			rootLogger.level = Level.WARN
		}

		test("get(arenaId) - skal returnere eksisterende record") {
			val testObject =
				ArenaDataIdTranslationDbo(
					amtId = UUID.randomUUID(),
					arenaTableName = "ARENA_TABLE_NAME",
					arenaId = "ARENA_ID",
				)

			dataIdTranslationRepository.insert(testObject)

			val stored = dataIdTranslationRepository.get(testObject.arenaTableName, testObject.arenaId)

			stored shouldNotBe null
			stored!!.amtId shouldBe testObject.amtId
			stored.arenaTableName shouldBe testObject.arenaTableName
			stored.arenaId shouldBe testObject.arenaId
		}

		test("get(amtId) - skal returnere arenaId") {
			val testObject =
				ArenaDataIdTranslationDbo(
					amtId = UUID.randomUUID(),
					arenaTableName = "ARENA_TABLE_NAME",
					arenaId = "ARENA_ID123",
				)
			dataIdTranslationRepository.insert(testObject)

			val stored = dataIdTranslationRepository.get(testObject.amtId)

			stored shouldNotBe null
			stored!!.arenaTableName shouldBe testObject.arenaTableName
			stored.arenaId shouldBe testObject.arenaId
		}
	})

package no.nav.amt.arena.acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class ArenaSakRepositoryTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var repository: ArenaSakRepository

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		repository = ArenaSakRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("skal inserte og hente sak") {
		val arenaId = 1234L

		repository.upsertSak(arenaId, 2020, 42, "1234")

		val sak = repository.hentSakMedArenaId(arenaId)

		sak shouldNotBe null

		sak?.let {
			it.id shouldNotBe null
			it.arenaSakId shouldBe arenaId
			it.aar shouldBe 2020
			it.lopenr shouldBe 42
			it.ansvarligEnhetId shouldBe "1234"
		}
	}

	test("upsertSak - skal oppdatere kun ansvarligEnhetId") {
		val arenaId = 1234L

		repository.upsertSak(arenaId, 2020, 42, "1234")
		repository.upsertSak(arenaId, 2021, 43, "5678")

		val sak = repository.hentSakMedArenaId(arenaId)

		sak shouldNotBe null

		sak?.let {
			it.arenaSakId shouldBe arenaId
			it.aar shouldBe 2020
			it.lopenr shouldBe 42
			it.ansvarligEnhetId shouldBe "5678"
		}
	}

})

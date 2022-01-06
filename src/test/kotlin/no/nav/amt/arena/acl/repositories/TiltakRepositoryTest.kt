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

class TiltakRepositoryTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var repository: TiltakRepository

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		repository = TiltakRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("Insert Should return Tiltak with Id") {
		val kode = "KODE"
		val navn = "NAVN"

		val tiltak = repository.upsert(kode, navn)

		tiltak.id shouldNotBe null
		tiltak.kode shouldBe kode
		tiltak.navn shouldBe navn

		val cache = repository.getCache()
		cache.stats().hitCount() shouldBe 0
		cache.getIfPresent(kode) shouldBe tiltak
	}

	test("Get multiple times should get item from cache") {
		val kode = "KODE"
		val navn = "NAVN"

		repository.upsert(kode, navn)

		val cache = repository.getCache()

		repository.getByKode(kode)
		cache.stats().hitCount() shouldBe 1

		repository.getByKode(kode)
		cache.stats().hitCount() shouldBe 2

	}

})

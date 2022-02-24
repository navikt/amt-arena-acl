package no.nav.amt.arena.acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

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
		val id = UUID.randomUUID()
		val kode = "KODE"
		val navn = "NAVN"

		repository.upsert(id, kode, navn)

		val tiltak = repository.getByKode(kode)

		tiltak?.id shouldBe id
		tiltak?.kode shouldBe kode
		tiltak?.navn shouldBe navn
	}

})

package no.nav.amt.arena.acl.integration

import no.nav.amt.arena.acl.database.DatabaseTestUtils.cleanDatabase
import no.nav.amt.arena.acl.database.SingletonPostgresContainer.postgresContainer
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import javax.sql.DataSource

@AutoConfigureJdbc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class JUnitRepositoryTestBase {
	@Autowired
	lateinit var flyway: Flyway

	@Autowired
	private lateinit var dataSource: DataSource

	@AfterAll
	fun afterAll() {
		flyway.clean()
	}

	@AfterEach
	fun cleanDatabase() {
		cleanDatabase(dataSource)
	}

	companion object {
		@ServiceConnection
		@Suppress("unused")
		val container = postgresContainer
	}
}

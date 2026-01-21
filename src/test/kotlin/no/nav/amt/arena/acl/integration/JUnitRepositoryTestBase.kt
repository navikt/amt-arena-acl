package no.nav.amt.arena.acl.integration

import no.nav.amt.arena.acl.database.DatabaseTestUtils.cleanDatabase
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureJdbc
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import javax.sql.DataSource

@AutoConfigureJdbc
abstract class JUnitRepositoryTestBase {
	@Autowired
	private lateinit var dataSource: DataSource

	@AfterEach
	fun cleanDatabase() = cleanDatabase(dataSource)

	companion object {
		@ServiceConnection
		private val container = SingletonPostgresContainer.postgresContainer
	}
}

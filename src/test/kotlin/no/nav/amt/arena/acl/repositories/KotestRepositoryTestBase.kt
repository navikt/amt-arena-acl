package no.nav.amt.arena.acl.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import no.nav.amt.arena.acl.database.DatabaseTestUtils.cleanDatabase
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import javax.sql.DataSource

@AutoConfigureJdbc
abstract class KotestRepositoryTestBase(
	body: FunSpec.() -> Unit,
) : FunSpec(body) {
	@Autowired
	private lateinit var dataSource: DataSource

	init {
		extensions(TestContainerSpecExtension(container))

		afterTest {
			cleanDatabase(dataSource)
		}
	}

	companion object {
		@ServiceConnection
		@Suppress("unused")
		private val container = SingletonPostgresContainer.postgresContainer
	}
}

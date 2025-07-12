package no.nav.amt.arena.acl.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.perSpec
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

@ActiveProfiles("test")
@AutoConfigureJdbc
abstract class KotestRepositoryTestBase(
	body: FunSpec.() -> Unit,
) : FunSpec(body) {
	@Autowired
	private lateinit var dataSource: DataSource

	init {
		listener(SingletonPostgresContainer.postgresContainer.perSpec())

		afterTest {
			DatabaseTestUtils.cleanDatabase(dataSource)
		}
	}

	companion object {
		@ServiceConnection
		@Suppress("unused")
		val container = SingletonPostgresContainer.postgresContainer
	}
}

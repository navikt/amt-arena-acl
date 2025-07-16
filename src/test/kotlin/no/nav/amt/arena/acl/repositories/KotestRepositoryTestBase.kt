package no.nav.amt.arena.acl.repositories

import io.kotest.core.spec.style.FunSpec
import no.nav.amt.arena.acl.database.DatabaseTestUtils.cleanDatabase
import no.nav.amt.arena.acl.database.SingletonPostgresContainer.postgresContainer
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import javax.sql.DataSource

@AutoConfigureJdbc
abstract class KotestRepositoryTestBase(
	body: FunSpec.() -> Unit,
) : FunSpec(body) {
	@Autowired
	private lateinit var dataSource: DataSource

	@Autowired
	private lateinit var flyway: Flyway

	init {
		afterSpec {
			flyway.clean()
		}

		afterTest {
			cleanDatabase(dataSource)
		}
	}

	companion object {
		@JvmStatic
		@DynamicPropertySource
		@Suppress("unused")
		fun overrideProps(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
			registry.add("spring.datasource.username", postgresContainer::getUsername)
			registry.add("spring.datasource.password", postgresContainer::getPassword)
			registry.add("spring.flyway.clean-disabled") { false }
		}
	}
}

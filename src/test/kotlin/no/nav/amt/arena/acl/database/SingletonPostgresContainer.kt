package no.nav.amt.arena.acl.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.sql.SQLException
import javax.sql.DataSource

object SingletonPostgresContainer {

	private val log = LoggerFactory.getLogger(javaClass)

	private const val POSTGRES_DOCKER_IMAGE_NAME = "postgres:14-alpine"

	private var postgresContainer: PostgreSQLContainer<Nothing>? = null

	private var containerDataSource: DataSource? = null

	fun getDataSource(): DataSource {
		return getDataSource(getContainer())
	}

	fun getContainer(): PostgreSQLContainer<Nothing> {
		if (postgresContainer == null) {
			log.info("Starting new postgres database...")

			val container = createContainer()
			postgresContainer = container

			container.start()

			log.info("Applying database migrations...")
			applyMigrations(getDataSource(container))

			setupShutdownHook()
		}

		return postgresContainer as PostgreSQLContainer<Nothing>
	}

	private fun getDataSource(container: PostgreSQLContainer<Nothing>): DataSource {
		if (!isValidDataSource(containerDataSource)) {
			containerDataSource = createDataSource(container)
		}

		return containerDataSource!!
	}

	private fun applyMigrations(dataSource: DataSource) {
		val flyway: Flyway = Flyway.configure()
			.dataSource(dataSource)
			.connectRetries(10)
			.cleanDisabled(false)
			.load()

		flyway.clean()
		flyway.migrate()
	}

	private fun createContainer(): PostgreSQLContainer<Nothing> {
		val container = PostgreSQLContainer<Nothing>(DockerImageName.parse(POSTGRES_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("postgres"))
		container.addEnv("TZ", "Europe/Oslo")
		return container.waitingFor(HostPortWaitStrategy())
	}

	private fun createDataSource(container: PostgreSQLContainer<Nothing>): DataSource {
		val config = HikariConfig()

		config.jdbcUrl = container.jdbcUrl
		config.username = container.username
		config.password = container.password

		return HikariDataSource(config)
	}

	private fun setupShutdownHook() {
		Runtime.getRuntime().addShutdownHook(Thread {
			log.info("Shutting down postgres database...")
			postgresContainer?.stop()
		})
	}

	private fun isValidDataSource(dataSource: DataSource?): Boolean {
		if (dataSource == null) return false

		return try {
			!dataSource.connection.isClosed
		} catch (e: SQLException) {
			false
		}
	}
}

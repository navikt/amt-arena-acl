package no.nav.amt.arena.acl.database

import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer

object SingletonPostgresContainer {
	private val log = LoggerFactory.getLogger(javaClass)

	val postgresContainer =
		PostgreSQLContainer<Nothing>(POSTGRES_DOCKER_IMAGE_NAME).apply {
			setupShutdownHook()
		}

	private fun setupShutdownHook() {
		Runtime.getRuntime().addShutdownHook(
			Thread {
				log.info("Shutting down postgres database...")
				postgresContainer.stop()
			},
		)
	}

	const val POSTGRES_DOCKER_IMAGE_NAME = "postgres:14-alpine"
}

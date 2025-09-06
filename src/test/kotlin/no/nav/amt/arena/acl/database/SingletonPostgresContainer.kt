package no.nav.amt.arena.acl.database

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

object SingletonPostgresContainer {
	private const val POSTGRES_DOCKER_IMAGE_NAME = "postgres:17-alpine"

	val postgresContainer = PostgreSQLContainer<Nothing>(
		DockerImageName
			.parse(POSTGRES_DOCKER_IMAGE_NAME)
			.asCompatibleSubstituteFor("postgres"),
	).apply {
		addEnv("TZ", "Europe/Oslo")
		waitingFor(Wait.forListeningPort())
	}
}

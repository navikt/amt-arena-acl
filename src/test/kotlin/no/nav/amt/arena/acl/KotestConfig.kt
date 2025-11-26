package no.nav.amt.arena.acl

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringExtension
import org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME

object KotestConfig : AbstractProjectConfig() {
	override suspend fun beforeProject() {
		System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "test")
	}

	override val extensions = listOf(
		SpringExtension(),
	)
}

package no.nav.amt.arena.acl.integration.utils

import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory

open class MockOAuthServer {
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		private val server = MockOAuth2Server()
		private const val AZURE_AD_ISSUER = "azuread"
	}

	fun start() {
		try {
			server.start()
		} catch (_: IllegalArgumentException) {
			log.info("${javaClass.simpleName} is already started")
		}
	}

	fun getDiscoveryUrl(issuer: String = AZURE_AD_ISSUER): String = server.wellKnownUrl(issuer).toString()
}

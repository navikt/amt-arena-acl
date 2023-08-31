package no.nav.amt.arena.acl.integration.utils

import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory

open class MockOAuthServer {

	private val azureAdIssuer = "azuread"

	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		private val server = MockOAuth2Server()
	}

	fun start() {
		try {
			server.start()
		} catch (e: IllegalArgumentException) {
			log.info("${javaClass.simpleName} is already started")
		}
	}

	fun getDiscoveryUrl(issuer: String = azureAdIssuer): String {
		return server.wellKnownUrl(issuer).toString()
	}

	fun shutdown() {
		server.shutdown()
	}

	fun issueAzureAdToken(
		subject: String = "test",
		audience: String = "test-aud",
		claims: Map<String, Any> = emptyMap()
	): String {
		return server.issueToken(azureAdIssuer, subject, audience, claims).serialize()
	}
}

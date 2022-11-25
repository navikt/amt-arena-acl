package no.nav.amt.arena.acl.clients.ordsproxy

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.utils.token_provider.ScopedTokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("default")
@Configuration
open class ArenaOrdsProxyClientConfiguration {

	@Value("\${amt-arena-ords-proxy.url}")
	lateinit var url: String

	@Value("\${amt-arena-ords-proxy.scope}")
	lateinit var scope: String

	@Bean
	open fun arenaOrdsProxyConnector(
		scopedTokenProvider: ScopedTokenProvider
	): ArenaOrdsProxyClient {
		return ArenaOrdsProxyClientImpl(
			arenaOrdsProxyUrl = url,
			tokenProvider = { scopedTokenProvider.getToken(scope) },
		)
	}

}

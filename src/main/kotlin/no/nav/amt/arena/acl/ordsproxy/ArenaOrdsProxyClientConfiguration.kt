package no.nav.amt.arena.acl.ordsproxy

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.ordsproxy.token_provider.ScopedTokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ArenaOrdsProxyClientConfiguration {

	@Value("\${poao-gcp-proxy.url}")
	lateinit var url: String

	@Value("\${poao-gcp-proxy.scope}")
	lateinit var scope: String

	@Bean
	open fun arenaOrdsProxyConnector(
		scopedTokenProvider: ScopedTokenProvider
	): ArenaOrdsProxyClient {
		return ArenaOrdsProxyClientImpl(
			tokenProvider = { scopedTokenProvider.getToken(scope) },
			arenaOrdsProxyUrl = "$url/proxy/amt-arena-ords-proxy"
		)
	}

}

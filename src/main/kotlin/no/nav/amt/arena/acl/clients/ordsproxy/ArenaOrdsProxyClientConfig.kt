package no.nav.amt.arena.acl.clients.ordsproxy

import ArenaOrdsProxyClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ArenaOrdsProxyClientConfig {

	@Value("\${amt-arena-ords-proxy.url}")
	lateinit var url: String

	@Value("\${amt-arena-ords-proxy.scope}")
	lateinit var scope: String

	@Bean
	open fun arenaOrdsProxyConnector(
		machineToMachineTokenClient: MachineToMachineTokenClient
	): ArenaOrdsProxyClient {
		return ArenaOrdsProxyClientImpl(
			arenaOrdsProxyUrl = url,
			tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		)
	}

}

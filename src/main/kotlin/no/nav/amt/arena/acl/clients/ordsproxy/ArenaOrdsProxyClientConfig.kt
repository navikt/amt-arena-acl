package no.nav.amt.arena.acl.clients.ordsproxy

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ArenaOrdsProxyClientConfig {
	@Bean
	fun arenaOrdsProxyConnector(
		machineToMachineTokenClient: MachineToMachineTokenClient,
		@Value($$"${amt-arena-ords-proxy.url}") url: String,
		@Value($$"${amt-arena-ords-proxy.scope}") scope: String,
	): ArenaOrdsProxyClient = ArenaOrdsProxyClientImpl(
		arenaOrdsProxyUrl = url,
		tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
	)
}

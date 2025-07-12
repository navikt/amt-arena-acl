package no.nav.amt.arena.acl.clients.ordsproxy

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ArenaOrdsProxyClientConfig {
	@Value("\${amt-arena-ords-proxy.url}")
	lateinit var url: String

	@Value("\${amt-arena-ords-proxy.scope}")
	lateinit var scope: String

	@Bean
	fun arenaOrdsProxyConnector(machineToMachineTokenClient: MachineToMachineTokenClient): ArenaOrdsProxyClient =
		ArenaOrdsProxyClientImpl(
			arenaOrdsProxyUrl = url,
			tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		)
}

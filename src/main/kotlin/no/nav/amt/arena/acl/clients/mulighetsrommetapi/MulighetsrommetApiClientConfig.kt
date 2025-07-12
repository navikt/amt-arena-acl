package no.nav.amt.arena.acl.clients.mulighetsrommetapi

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MulighetsrommetApiClientConfig {
	@Value("\${mulighetsrommet-api.url}")
	lateinit var url: String

	@Value("\${mulighetsrommet-api.scope}")
	lateinit var scope: String

	@Bean
	fun mulighetsrommetApiClient(machineToMachineTokenClient: MachineToMachineTokenClient): MulighetsrommetApiClient =
		MulighetsrommetApiClientImpl(
			baseUrl = url,
			tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		)
}

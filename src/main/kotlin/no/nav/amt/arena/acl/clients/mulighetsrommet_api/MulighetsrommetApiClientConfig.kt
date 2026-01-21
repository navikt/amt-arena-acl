package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MulighetsrommetApiClientConfig {
	@Bean
	fun mulighetsrommetApiClient(
		machineToMachineTokenClient: MachineToMachineTokenClient,
		@Value($$"${mulighetsrommet-api.url}") url: String,
		@Value($$"${mulighetsrommet-api.scope}") scope: String,
	) = MulighetsrommetApiClient(
		baseUrl = url,
		tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
	)
}

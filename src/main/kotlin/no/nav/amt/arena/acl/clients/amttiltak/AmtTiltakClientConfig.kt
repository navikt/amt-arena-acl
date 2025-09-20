package no.nav.amt.arena.acl.clients.amttiltak

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class AmtTiltakClientConfig {
	@Bean
	fun amtTiltakClient(
		machineToMachineTokenClient: MachineToMachineTokenClient,
		@Value($$"${amt-tiltak.url}") url: String,
		@Value($$"${amt-tiltak.scope}") scope: String,
	) = AmtTiltakClient(
		baseUrl = url,
		tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
	)
}

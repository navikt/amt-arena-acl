package no.nav.amt.arena.acl.clients.amttiltak

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AmtTiltakClientConfig {
	@Value("\${amt-tiltak.url}")
	lateinit var url: String

	@Value("\${amt-tiltak.scope}")
	lateinit var scope: String

	@Bean
	fun amtTiltakClient(machineToMachineTokenClient: MachineToMachineTokenClient): AmtTiltakClient =
		AmtTiltakClientImpl(
			baseUrl = url,
			tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		)
}

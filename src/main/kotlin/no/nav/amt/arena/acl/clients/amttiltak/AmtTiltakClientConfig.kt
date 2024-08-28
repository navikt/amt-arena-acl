package no.nav.amt.arena.acl.clients.amttiltak

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class AmtTiltakClientConfig {

	@Value("\${amt-tiltak.url}")
	lateinit var url: String

	@Value("\${amt-tiltak.scope}")
	lateinit var scope: String

	@Bean
	open fun amtTiltakClient(machineToMachineTokenClient: MachineToMachineTokenClient): AmtTiltakClient {
		return AmtTiltakClientImpl(
			baseUrl = url,
			tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		)
	}

}

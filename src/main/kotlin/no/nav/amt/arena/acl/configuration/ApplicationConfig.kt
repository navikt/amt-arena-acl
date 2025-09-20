package no.nav.amt.arena.acl.configuration

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@EnableJwtTokenValidation
@Configuration(proxyBeanMethods = false)
class ApplicationConfig {
	@Bean
	fun machineToMachineTokenClient(
		@Value($$"${nais.env.azureAppClientId}") azureAdClientId: String,
		@Value($$"${nais.env.azureOpenIdConfigTokenEndpoint}") azureTokenEndpoint: String,
		@Value($$"${nais.env.azureAppJWK}") azureAdJWK: String,
	): MachineToMachineTokenClient = AzureAdTokenClientBuilder
		.builder()
		.withClientId(azureAdClientId)
		.withTokenEndpointUrl(azureTokenEndpoint)
		.withPrivateJwk(azureAdJWK)
		.buildMachineToMachineTokenClient()

	@Bean
	@Profile("default")
	fun unleashClient(
		@Value($$"${app.env.unleashUrl}") unleashUrl: String,
		@Value($$"${app.env.unleashApiToken}") unleashApiToken: String,
	): DefaultUnleash = DefaultUnleash(
		UnleashConfig
			.builder()
			.appName(APP_NAME)
			.instanceId(APP_NAME)
			.unleashAPI(unleashUrl)
			.apiKey(unleashApiToken)
			.build()
	)

	companion object {
		const val APP_NAME = "amt-arena-acl"
	}
}

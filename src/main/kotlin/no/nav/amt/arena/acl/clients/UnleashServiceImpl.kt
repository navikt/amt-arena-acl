package no.nav.amt.arena.acl.clients

import io.getunleash.Unleash
import org.springframework.stereotype.Service

@Service
class UnleashServiceImpl(
	private val unleashClient: Unleash
) {
	fun disableArenaDeltakerConsumer(): Boolean {
		return unleashClient.isEnabled("amt.disable-arena-deltaker-consumer")
	}
}

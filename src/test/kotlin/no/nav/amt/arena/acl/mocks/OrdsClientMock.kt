package no.nav.amt.arena.acl.mocks

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.clients.ordsproxy.Arbeidsgiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class OrdsClientMock {

	companion object {
		val fnrHandlers = mutableMapOf<String, () -> String?>()
		val virksomhetsHandler = mutableMapOf<String, () -> String>()
	}

	@Bean
	open fun ordsProxyClient(): ArenaOrdsProxyClient {

		return object : ArenaOrdsProxyClient {
			override fun hentFnr(arenaPersonId: String): String? {
				if (fnrHandlers[arenaPersonId] != null) {
					return fnrHandlers[arenaPersonId]!!.invoke()
				}

				return "12345"

			}

			override fun hentArbeidsgiver(arenaArbeidsgiverId: String): Arbeidsgiver? {
				throw NotImplementedError()
			}

			override fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String {
				if (virksomhetsHandler[arenaArbeidsgiverId] != null) {
					return virksomhetsHandler[arenaArbeidsgiverId]!!.invoke()
				}

				return "12345"
			}

		}
	}


}

package no.nav.amt.arena.acl.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ToggleService(
	@Value("\${toggle.hent_gjennomforing_fra_mulighetsrommet}")
	private val hentGjennomforingFraMulighetsrommet: String,
) {
	fun hentGjennomforingFraMulighetsrommetEnabled(): Boolean {
		return hentGjennomforingFraMulighetsrommet == "true"
	}

}

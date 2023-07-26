package no.nav.amt.arena.acl.controller

import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api")
class Controller(
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService
) {

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@GetMapping("/arenaId")
	fun hentArenaId(@PathVariable("id") id: UUID): String? {
		return arenaDataIdTranslationService.hentArenaId(id)
	}

}

package no.nav.amt.arena.acl.controller

import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api")
class Controller(
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService
) {

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@GetMapping("/translation/{id}")
	fun hentArenaId(@PathVariable("id") id: UUID): HentArenaIdResponse {
		return arenaDataIdTranslationService.hentArenaIdEllerHistId(id)
			?.let { HentArenaIdResponse(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke arena id for $id")
	}

	data class HentArenaIdResponse(
		val arenaId: String
	)

}

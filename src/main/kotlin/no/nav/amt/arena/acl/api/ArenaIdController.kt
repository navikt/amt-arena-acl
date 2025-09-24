package no.nav.amt.arena.acl.api

import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.utils.AZURE_AD_ISSUER
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
class ArenaIdController(
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService
) {
	@ProtectedWithClaims(issuer = AZURE_AD_ISSUER)
	@GetMapping("/translation/{id}")
	fun hentArenaId(@PathVariable("id") id: UUID): HentArenaIdResponse =
		arenaDataIdTranslationService.hentArenaId(id)
			?.let { HentArenaIdResponse(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke arena id for $id")

	@ProtectedWithClaims(issuer = AZURE_AD_ISSUER)
	@GetMapping("/v2/translation/{id}")
	fun hentArenaIdV2(@PathVariable("id") id: UUID): HentArenaIdV2Response =
		arenaDataIdTranslationService.hentArenaId(id)
			?.let { arenaId ->
				HentArenaIdV2Response(arenaId = arenaId, arenaHistId = null)
			}
			?: HentArenaIdV2Response(
				arenaId = null,
				arenaHistId = arenaDataIdTranslationService.hentArenaHistId(id)
			)
}

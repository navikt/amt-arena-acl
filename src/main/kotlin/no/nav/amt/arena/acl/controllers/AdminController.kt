package no.nav.amt.arena.acl.controllers

import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/admin")
class AdminController(
	val arenaDataRepository: ArenaDataRepository
) {

	@GetMapping("/reingest")
	fun reingest() {
		arenaDataRepository.retryAll()
	}

}

package no.nav.amt.arena.acl.api

import jakarta.servlet.http.HttpServletRequest
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Unprotected
@RestController
@RequestMapping("/internal/api")
class InternalAPI(
	val kafkaProducerService: KafkaProducerService
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@PostMapping("/tombstone-enkeltplass-deltaker")
	fun tombstoneEnkeltplassDeltaker(
		request: HttpServletRequest,
		@RequestBody body: DeltakereRequest
	) {
		if (!isInternal(request)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}

		body.deltakere.forEach { deltakerId ->
			log.info("Tombstone deltaker med id $deltakerId")
			kafkaProducerService.tombstoneEnkeltplassDeltaker(deltakerId)
		}
	}


	private fun isInternal(request: HttpServletRequest): Boolean {
		return request.remoteAddr == "127.0.0.1"
	}

	data class DeltakereRequest(
		val deltakere: List<UUID>
	)
}

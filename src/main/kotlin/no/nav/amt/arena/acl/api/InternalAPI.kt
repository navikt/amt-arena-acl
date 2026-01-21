package no.nav.amt.arena.acl.api

import jakarta.servlet.http.HttpServletRequest
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.common.job.JobRunner
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
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
	val kafkaProducerService: KafkaProducerService,
	private val arenaDataRepository: ArenaDataRepository
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

	@PostMapping("/relast-deltakere/{tiltakskode}")
	fun relastDeltakere(
		request: HttpServletRequest,
		@PathVariable tiltakskode: String,
	) {
		if (!isInternal(request)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
		if (tiltakskode !in setOf("ENKELAMO", "ENKFAGYRKE", "HOYEREUTD")) {
			throw IllegalArgumentException("Kan ikke relaste tiltakstype $tiltakskode. Det er bare trygt å relaste tiltakstyper som komet ikke er master for")
		}
		log.info("Retryer deltakere med tiltakskode=$tiltakskode")
		JobRunner.runAsync("republiser_deltakere_kafka") {
			arenaDataRepository.retryDeltakerePaaTiltakstype(tiltakskode)
		}

		log.info("Done: Retryer deltakere med tiltakskode=$tiltakskode")

	}

	private fun isInternal(request: HttpServletRequest): Boolean {
		return request.remoteAddr == "127.0.0.1"
	}

	data class DeltakereRequest(
		val deltakere: List<UUID>
	)
}

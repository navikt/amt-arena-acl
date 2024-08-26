package no.nav.amt.arena.acl.controller

import jakarta.servlet.http.HttpServletRequest
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.processors.DeltakerProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Unprotected
@RestController
@RequestMapping("/internal/api")
class InternalController(
	private val arenaDataRepository: ArenaDataRepository,
	private val deltakerProcessor: DeltakerProcessor,
	private val kafkaProducerService: KafkaProducerService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@GetMapping("/rekonstruer-slettede-deltakere")
	fun fjernTilgangerHosArrangor(
		request: HttpServletRequest,
	) {
		if (!isInternal(request)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}

		val slettedeDeltakere = arenaDataRepository.getDeleteDeltakerRecords()
		log.info("Fant ${slettedeDeltakere.size} slettede deltakere")
		slettedeDeltakere.forEach {
			handleSlettetDeltaker(it)
		}
		log.info("Ferdig med å håndtere slettede deltakere")
	}

	private fun handleSlettetDeltaker(arenaDataDbo: ArenaDataDbo) {
		val arenaDeltakerRaw = arenaDataDbo.before?.let { fromJsonString<ArenaDeltaker>(it) }
			?: throw IllegalStateException("Slettet deltaker med arenaId ${arenaDataDbo.arenaId} mangler before")
		val arenaDeltakerId = arenaDeltakerRaw.TILTAKDELTAKER_ID.toString()
		val arenaGjennomforingId = arenaDeltakerRaw.TILTAKGJENNOMFORING_ID.toString()

		if (!arenaDeltakerRaw.EKSTERN_ID.isNullOrEmpty()) {
			log.info("Ignorerer deltaker som har eksternid ${arenaDeltakerRaw.EKSTERN_ID}")
			return
		}

		val gjennomforing = try {
			deltakerProcessor.getGjennomforing(arenaGjennomforingId)
		} catch (e: Exception) {
			log.error("${e.message}, arenaid $arenaDeltakerId")
			return
		}
		val deltaker = deltakerProcessor.createDeltaker(arenaDeltakerRaw, gjennomforing)

		val deltakerKafkaMessage = AmtKafkaMessageDto(
			type = PayloadType.DELTAKER,
			operation = AmtOperation.CREATED,
			payload = deltaker
		)
		kafkaProducerService.sendTilAmtTiltak(deltaker.id, deltakerKafkaMessage)

		log.info("Melding for tidligere slettet deltaker id=${deltaker.id} arenaId=$arenaDeltakerId transactionId=${deltakerKafkaMessage.transactionId} op=${deltakerKafkaMessage.operation} er sendt")
	}

	private fun isInternal(request: HttpServletRequest): Boolean {
		return request.remoteAddr == "127.0.0.1"
	}
}

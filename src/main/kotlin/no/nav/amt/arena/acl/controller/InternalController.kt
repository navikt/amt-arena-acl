package no.nav.amt.arena.acl.controller

import ArenaOrdsProxyClient
import jakarta.servlet.http.HttpServletRequest
import no.nav.amt.arena.acl.clients.amttiltak.AmtTiltakClient
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaHistDeltaker
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.processors.DeltakerProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.ARENA_HIST_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.amt.arena.acl.utils.tryRun
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

@Unprotected
@RestController
@RequestMapping("/internal/api")
class InternalController(
	private val arenaDataRepository: ArenaDataRepository,
	private val deltakerProcessor: DeltakerProcessor,
	private val kafkaProducerService: KafkaProducerService,
	private val amtTiltakClient: AmtTiltakClient,
	private val ordsClient: ArenaOrdsProxyClient,
	private val arenaDataIdTranslationRepository: ArenaDataIdTranslationRepository
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@GetMapping("/id-hist-deltakere")
	fun lagreIdForHistDeltakere(
		request: HttpServletRequest,
	) {
		if (!isInternal(request)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}

		val handledHistDeltakere = arenaDataRepository.getHandledHistDeltakerRecords()
		log.info("Fant ${handledHistDeltakere.size} hist-deltakere med status HANDLED")
		handledHistDeltakere.forEach {
			fetchAndSaveAmtIdForHistDeltaker(it)
		}
		log.info("Ferdig med å håndtere id-er for hist-deltakere")
	}

	private fun fetchAndSaveAmtIdForHistDeltaker(arenaDataDbo: ArenaDataDbo) {
		val arenaDeltakerRaw = arenaDataDbo.after?.let { fromJsonString<ArenaHistDeltaker>(it) }
			?: throw IllegalStateException("Hist-deltaker med arenaId ${arenaDataDbo.arenaId} mangler after")
		val arenaDeltakerId = arenaDeltakerRaw.HIST_TILTAKDELTAKER_ID.toString()
		val arenaGjennomforingId = arenaDeltakerRaw.TILTAKGJENNOMFORING_ID.toString()

		val gjennomforing = try {
			deltakerProcessor.getGjennomforing(arenaGjennomforingId)
		} catch (e: Exception) {
			log.error("${e.message}, arenaid $arenaDeltakerId")
			return
		}
		val arenaDeltaker = arenaDeltakerRaw
			.tryRun { it.mapTiltakDeltaker() }
			.getOrThrow()

		val personIdent = ordsClient.hentFnr(arenaDeltaker.personId)
			?: throw ValidationException("Arena mangler personlig ident for personId=${arenaDeltaker.personId}")

		val deltakerIdFraAmtTiltak = getDeltakerId(
			personIdent = personIdent,
			gjennomforingId = gjennomforing.id,
			startdato = arenaDeltaker.datoFra,
			sluttdato = arenaDeltaker.datoTil
		)

		if (deltakerIdFraAmtTiltak != null) {
			if (arenaDataIdTranslationRepository.get(deltakerIdFraAmtTiltak) != null) {
				log.warn("amt-id er allerede lagret for arenaId $arenaDeltakerId: $deltakerIdFraAmtTiltak")
			} else {
				arenaDataIdTranslationRepository.insert(ArenaDataIdTranslationDbo(
					amtId = deltakerIdFraAmtTiltak,
					arenaTableName = ARENA_HIST_DELTAKER_TABLE_NAME,
					arenaId = arenaDeltakerId
				))
				log.info("Lagret amt-id $deltakerIdFraAmtTiltak for arenaid $arenaDeltakerId")
			}
		} else {
			log.warn("Fant ikke amt-id for hist-deltaker med arenaid $arenaDeltakerId")
		}
	}

	private fun getDeltakerId(
		personIdent: String,
		gjennomforingId: UUID,
		startdato: LocalDate?,
		sluttdato: LocalDate?
	): UUID? {
		val deltakelser = amtTiltakClient.hentDeltakelserForPerson(personIdent)
		return deltakelser.find { it.gjennomforing.id == gjennomforingId && it.startDato == startdato && it.sluttDato == sluttdato }?.id
	}

	@GetMapping("/rekonstruer-slettede-deltakere")
	fun rekonstruerSlettedeDeltakere(
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

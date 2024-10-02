package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.clients.amttiltak.AmtTiltakClient
import no.nav.amt.arena.acl.clients.amttiltak.DeltakerDto
import no.nav.amt.arena.acl.clients.amttiltak.DeltakerStatusDto
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaHistDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaHistDeltakerKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.exceptions.ExternalSourceSystemException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.tryRun
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
open class HistDeltakerProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val ordsClient: ArenaOrdsProxyClient,
	private val kafkaProducerService: KafkaProducerService,
	private val deltakerProcessor: DeltakerProcessor,
	private val amtTiltakClient: AmtTiltakClient,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService
) : ArenaMessageProcessor<ArenaHistDeltakerKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaHistDeltakerKafkaMessage) {
		val arenaDeltakerRaw = message.getData()
		val arenaDeltakerId = arenaDeltakerRaw.HIST_TILTAKDELTAKER_ID.toString()
		val arenaGjennomforingId = arenaDeltakerRaw.TILTAKGJENNOMFORING_ID.toString()
		val gjennomforing = deltakerProcessor.getGjennomforing(arenaGjennomforingId)

		externalDeltakerGuard(arenaDeltakerRaw)

		if (message.operationType != AmtOperation.CREATED) {
			log.info("Mottatt melding for hist-deltaker arenaHistId=$arenaDeltakerId op=${message.operationType}, blir ikke behandlet")
			throw IgnoredException("Ignorerer hist-deltaker som har operation type ${message.operationType}")
		} else {
			val histDeltaker = arenaDeltakerRaw
				.tryRun { it.mapTiltakDeltaker() }
				.getOrThrow()

			val personIdent = ordsClient.hentFnr(histDeltaker.personId)
				?: throw ValidationException("Arena mangler personlig ident for personId=${histDeltaker.personId}")

			val deltakerFraAmtTiltak = getAmtDeltaker(
				personIdent = personIdent,
				gjennomforingId = gjennomforing.id,
				startdato = histDeltaker.datoFra,
				sluttdato = histDeltaker.datoTil,
			) ?: throw ValidationException("Fant ikke amt-deltaker for hist-deltaker med arenaid $arenaDeltakerId")

			arenaDataIdTranslationService.lagreHistDeltakerId(amtDeltakerId = deltakerFraAmtTiltak.id, histDeltakerArenaId = arenaDeltakerId)

			if (deltakerFraAmtTiltak.status == DeltakerStatusDto.FEILREGISTRERT) {
				log.info("amt-deltaker ${deltakerFraAmtTiltak.id} er feilregistrert, gjenoppretter")
				gjenopprettFeilregistrertDeltaker(histDeltaker, deltakerFraAmtTiltak.id, gjennomforing, personIdent)
			}
			arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaDeltakerId))
		}
	}

	private fun externalDeltakerGuard(arenaDeltakerRaw: ArenaHistDeltaker) {
		val deltakerHistId = arenaDeltakerRaw.HIST_TILTAKDELTAKER_ID.toString()
		if (!arenaDeltakerRaw.EKSTERN_ID.isNullOrEmpty()) {
			val eksternId = UUID.fromString(arenaDeltakerRaw.EKSTERN_ID)

			val arenaId = arenaDataIdTranslationService.hentArenaHistId(eksternId)
			if (arenaId == null) {
				arenaDataIdTranslationService.lagreHistDeltakerId(amtDeltakerId = eksternId, histDeltakerArenaId = deltakerHistId)
			}
			else if (arenaId != deltakerHistId) {
				throw ValidationException("Fikk arenadeltaker med id $deltakerHistId og EKSTERN_ID ${arenaDeltakerRaw.EKSTERN_ID} men arenaId er allerede mappet til $arenaId")
			}

			throw ExternalSourceSystemException("hist-deltaker har eksternid ${arenaDeltakerRaw.EKSTERN_ID}")
		}
		if (arenaDeltakerRaw.DELTAKERTYPEKODE == "EKSTERN") {
			throw ExternalSourceSystemException("hist-deltaker har deltakertypekode ekstern, arenaid $deltakerHistId")
		}

	}

	private fun gjenopprettFeilregistrertDeltaker(
		arenaDeltaker: TiltakDeltaker,
		amtDeltakerId: UUID,
		gjennomforing: Gjennomforing,
		personIdent: String
	) {
		val deltaker = arenaDeltaker.constructDeltaker(
			amtDeltakerId = amtDeltakerId,
			gjennomforingId = gjennomforing.id,
			gjennomforingSluttDato = gjennomforing.sluttDato,
			erGjennomforingAvsluttet = gjennomforing.erAvsluttet(),
			erKurs = gjennomforing.erKurs(),
			personIdent = personIdent,
		)
		val deltakerKafkaMessage = AmtKafkaMessageDto(
			type = PayloadType.DELTAKER,
			operation = AmtOperation.MODIFIED,
			payload = deltaker
		)

		kafkaProducerService.sendTilAmtTiltak(deltaker.id, deltakerKafkaMessage)
		log.info("Melding for hist-deltaker id=${deltaker.id} arenaHistId=${arenaDeltaker.tiltakdeltakerId} transactionId=${deltakerKafkaMessage.transactionId} op=${deltakerKafkaMessage.operation} er sendt")
	}

	private fun getAmtDeltaker(
		personIdent: String,
		gjennomforingId: UUID,
		startdato: LocalDate?,
		sluttdato: LocalDate?
	): DeltakerDto? {
		val deltakelser = amtTiltakClient.hentDeltakelserForPerson(personIdent)
		return deltakelser.find { it.gjennomforing.id == gjennomforingId && it.startDato == startdato && it.sluttDato == sluttdato }
	}
}

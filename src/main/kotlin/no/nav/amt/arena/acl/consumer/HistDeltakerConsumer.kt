package no.nav.amt.arena.acl.consumer

import no.nav.amt.arena.acl.clients.amttiltak.AmtTiltakClient
import no.nav.amt.arena.acl.clients.amttiltak.DeltakerDto
import no.nav.amt.arena.acl.clients.amttiltak.DeltakerStatus
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
import no.nav.amt.arena.acl.clients.ordsproxy.ArenaOrdsProxyClientImpl
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.amt.erAvsluttende
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaHistDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaHistDeltakerKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.exceptions.ExternalSourceSystemException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.exceptions.OperationNotImplementedException
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.DeltakerDbo
import no.nav.amt.arena.acl.repositories.DeltakerRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.asLocalDate
import no.nav.amt.arena.acl.utils.asLocalDateTime
import no.nav.amt.arena.acl.utils.tryRun
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class HistDeltakerConsumer(
	private val arenaDataRepository: ArenaDataRepository,
	private val ordsClient: ArenaOrdsProxyClientImpl,
	private val kafkaProducerService: KafkaProducerService,
	private val arenaDeltakerConsumer: ArenaDeltakerConsumer,
	private val amtTiltakClient: AmtTiltakClient,
	private val deltakerRepository: DeltakerRepository,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
) : ArenaMessageConsumer<ArenaHistDeltakerKafkaMessage> {
	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaHistDeltakerKafkaMessage) {
		val arenaDeltakerRaw = message.getData()
		val arenaHistDeltakerId = arenaDeltakerRaw.HIST_TILTAKDELTAKER_ID.toString()
		val arenaGjennomforingId = arenaDeltakerRaw.TILTAKGJENNOMFORING_ID.toString()
		val gjennomforing = arenaDeltakerConsumer.getGjennomforing(arenaGjennomforingId)

		externalDeltakerGuard(arenaDeltakerRaw)

		if (message.operationType != AmtOperation.CREATED) {
			log.info("Mottatt melding for hist-deltaker arenaHistId=$arenaHistDeltakerId op=${message.operationType}, blir ikke behandlet")
			throw IgnoredException("Ignorerer hist-deltaker som har operation type ${message.operationType}")
		} else {
			val histDeltaker =
				arenaDeltakerRaw
					.tryRun { it.mapTiltakDeltaker() }
					.getOrThrow()

			val personIdent =
				ordsClient.hentFnr(histDeltaker.personId)
					?: throw ValidationException("Arena mangler personlig ident for personId=${histDeltaker.personId}")

			val eksisterendeDeltaker = getMatchingDeltaker(arenaDeltakerRaw)

			if (eksisterendeDeltaker == null) {
				// Her havner vi når vi spiller av topicen fra start
				// (fordi disse deltakerene er allerede fjernet fra arenas deltaker tabell)
				val nyDeltaker = arenaDeltakerConsumer.createDeltaker(histDeltaker, gjennomforing, erHistDeltaker = true)

				nyDeltaker.validerGyldigHistDeltaker()
				log.info(
					"Fant ingen match for hist-deltaker $arenaHistDeltakerId, oppretter ny deltaker ${nyDeltaker.id} og lagrer mapping og sender videre",
				)
				sendMessage(nyDeltaker, arenaHistDeltakerId)
			} else {
				log.info("Hist deltaker $arenaHistDeltakerId matcher deltaker ${eksisterendeDeltaker.arenaId}")

				val eksisterendeDeltakerAmtId =
					arenaDataIdTranslationService.hentAmtId(eksisterendeDeltaker.arenaId.toString())
						?: throw ValidationException(
							"Fant matchende deltaker for hist deltaker $arenaHistDeltakerId men fant ikke deltakeren igjen i translation tabellen",
						)
				val deltakerFraAmtTiltak =
					getAmtDeltaker(eksisterendeDeltakerAmtId, personIdent)
						?: throw ValidationException(
							"Fant matchende deltaker for hist deltaker $arenaHistDeltakerId men fant ikke deltakeren igjen i amt-tiltak",
						)

				arenaDataIdTranslationService.lagreHistDeltakerId(
					amtDeltakerId = deltakerFraAmtTiltak.id,
					histDeltakerArenaId = arenaHistDeltakerId,
				)
				if (deltakerFraAmtTiltak.status == DeltakerStatus.FEILREGISTRERT) {
					log.info("amt-deltaker ${deltakerFraAmtTiltak.id} er feilregistrert, gjenoppretter")
					gjenopprettFeilregistrertDeltaker(histDeltaker, deltakerFraAmtTiltak.id, gjennomforing, personIdent)
				}
			}
			arenaDataRepository.upsert(
				message.toUpsertInputWithStatusHandled(arenaHistDeltakerId, note = "Fant match? ${eksisterendeDeltaker != null}"),
			)
		}
	}

	private fun externalDeltakerGuard(arenaDeltakerRaw: ArenaHistDeltaker) {
		val deltakerHistId = arenaDeltakerRaw.HIST_TILTAKDELTAKER_ID.toString()
		if (!arenaDeltakerRaw.EKSTERN_ID.isNullOrEmpty()) {
			val eksternId = UUID.fromString(arenaDeltakerRaw.EKSTERN_ID)

			val arenaId = arenaDataIdTranslationService.hentArenaHistId(eksternId)
			if (arenaId == null) {
				arenaDataIdTranslationService.lagreHistDeltakerId(
					amtDeltakerId = eksternId,
					histDeltakerArenaId = deltakerHistId,
				)
			} else if (arenaId != deltakerHistId) {
				log.error(
					"Fikk arenadeltaker med id $deltakerHistId og EKSTERN_ID ${arenaDeltakerRaw.EKSTERN_ID} men arenaId er allerede mappet til $arenaId",
				)
				throw ValidationException(
					"Fikk arena hist-deltaker med id $deltakerHistId og EKSTERN_ID ${arenaDeltakerRaw.EKSTERN_ID} men arenaId er allerede mappet til $arenaId",
				)
			}

			throw ExternalSourceSystemException("hist-deltaker har eksternid ${arenaDeltakerRaw.EKSTERN_ID}")
		}
		if (arenaDeltakerRaw.DELTAKERTYPEKODE == "EKSTERN") {
			throw ExternalSourceSystemException("hist-deltaker har deltakertypekode ekstern, arenaid $deltakerHistId")
		}
	}

	private fun sendMessage(
		deltaker: AmtDeltaker,
		arenaDeltakerId: String,
	) {
		val deltakerKafkaMessage =
			AmtKafkaMessageDto(
				type = PayloadType.DELTAKER,
				operation = AmtOperation.CREATED,
				payload = deltaker,
			)
		kafkaProducerService.sendTilAmtTiltak(deltaker.id, deltakerKafkaMessage)
		log.info(
			"Melding for deltaker id=${deltaker.id} arenaId=$arenaDeltakerId transactionId=${deltakerKafkaMessage.transactionId} op=${deltakerKafkaMessage.operation} er sendt",
		)
	}

	private fun gjenopprettFeilregistrertDeltaker(
		arenaDeltaker: TiltakDeltaker,
		amtDeltakerId: UUID,
		gjennomforing: Gjennomforing,
		personIdent: String,
	) {
		val deltaker =
			arenaDeltaker.constructDeltaker(
				amtDeltakerId = amtDeltakerId,
				gjennomforingId = gjennomforing.id,
				gjennomforingSluttDato = gjennomforing.sluttDato,
				erGjennomforingAvsluttet = gjennomforing.erAvsluttet(),
				deltakelseKreverGodkjenningLoep = gjennomforing.erKurs() || gjennomforing.erEnkelplass(),
				personIdent = personIdent,
			)
		val deltakerKafkaMessage =
			AmtKafkaMessageDto(
				type = PayloadType.DELTAKER,
				operation = AmtOperation.MODIFIED,
				payload = deltaker,
			)
		deltaker.validerGyldigHistDeltaker()
		kafkaProducerService.sendTilAmtTiltak(deltaker.id, deltakerKafkaMessage)
		log.info(
			"Melding for hist-deltaker id=${deltaker.id} arenaHistId=${arenaDeltaker.tiltakdeltakerId} transactionId=${deltakerKafkaMessage.transactionId} op=${deltakerKafkaMessage.operation} er sendt",
		)
	}

	private fun getAmtDeltaker(
		id: UUID,
		personIdent: String,
	): DeltakerDto? {
		val deltakelser = amtTiltakClient.hentDeltakelserForPerson(personIdent)
		return deltakelser
			.find { it.id == id }
	}

	private fun getMatchingDeltaker(arenaHistDeltaker: ArenaHistDeltaker): DeltakerDbo? {
		val personId =
			arenaHistDeltaker.PERSON_ID
				?: throw ValidationException("Kan ikke matche hist deltaker som mangler PERSON_ID")
		val datoStatusendring =
			arenaHistDeltaker.DATO_STATUSENDRING?.asLocalDateTime()
				?: throw ValidationException("Kan ikke matche hist deltaker som mangler DATO_STATUSENDRING")
		val datoFra = arenaHistDeltaker.DATO_FRA?.asLocalDate()
		val datoTil = arenaHistDeltaker.DATO_TIL?.asLocalDate()
		val arenaDeltakere =
			deltakerRepository
				.getDeltakereForPerson(personId, arenaHistDeltaker.TILTAKGJENNOMFORING_ID)

		val matchendeDeltakere =
			arenaDeltakere
				.filter { it.datoFra == datoFra }
				.filter { it.datoTil == datoTil }
				.filter { it.datoStatusEndring == datoStatusendring }

		if (arenaDeltakere.isEmpty()) {
			log.info(
				"Fant ingen match for hist deltaker med id ${arenaHistDeltaker.HIST_TILTAKDELTAKER_ID} " +
					"fordi personen har ingen andre deltakelser i databasen",
			)
			return null
		} else if (matchendeDeltakere.isEmpty()) {
			log.info(
				"Fant ingen match for hist-deltaker med id ${arenaHistDeltaker.HIST_TILTAKDELTAKER_ID}. " +
					"fradato: $datoFra, tildato: $datoTil, modDato: $datoStatusendring" +
					"Personen har ${arenaDeltakere.size} andre deltakelser",
			)

			arenaDeltakere.forEach {
				log.info("Ingen match med arenaId: ${it.arenaId}, fradato: ${it.datoFra}, tildato ${it.datoTil}, ${it.modDato}")
			}
			return null
		} else if (matchendeDeltakere.size == 1) {
			log.info("hist deltaker med ${arenaHistDeltaker.HIST_TILTAKDELTAKER_ID} matcher med ${matchendeDeltakere.first().arenaId}")
			return matchendeDeltakere.first()
		}

		throw OperationNotImplementedException(
			"Fant ${arenaDeltakere.size} deltakere som matcher med hist deltaker ${arenaHistDeltaker.HIST_TILTAKDELTAKER_ID}",
		)
	}

	private fun AmtDeltaker.validerGyldigHistDeltaker() {
		if (!status.erAvsluttende()) {
			throw IllegalStateException("Hist deltaker har fått status $status")
		}
		if (statusEndretDato == null) {
			throw ValidationException(
				"Kan ikke sende videre hist-deltaker $id fordi den mangler statusEndretDato som vil utledes til LocalDateTime.now()",
			)
		}
	}
}

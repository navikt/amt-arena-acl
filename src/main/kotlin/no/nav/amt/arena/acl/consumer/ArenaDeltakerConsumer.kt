package no.nav.amt.arena.acl.consumer

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.MulighetsrommetApiClient
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.DependencyNotValidException
import no.nav.amt.arena.acl.exceptions.ExternalSourceSystemException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.metrics.DeltakerMetricHandler
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.DeltakerRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.tryRun
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Component
open class ArenaDeltakerConsumer(
	private val arenaDataRepository: ArenaDataRepository,
	private val deltakerRepository: DeltakerRepository,
	private val gjennomforingService: GjennomforingService,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val metrics: DeltakerMetricHandler,
	private val kafkaProducerService: KafkaProducerService,
	private val mulighetsrommetApiClient: MulighetsrommetApiClient
) : ArenaMessageConsumer<ArenaDeltakerKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltakerRaw = message.getData()
		val arenaDeltakerId = arenaDeltakerRaw.TILTAKDELTAKER_ID.toString()
		val arenaGjennomforingId = arenaDeltakerRaw.TILTAKGJENNOMFORING_ID.toString()
		val gjennomforing = getGjennomforing(arenaGjennomforingId)

		externalDeltakerGuard(arenaDeltakerRaw)

		val arenaDeltaker = arenaDeltakerRaw
			.tryRun { it.mapTiltakDeltaker() }
			.getOrThrow()

		val deltaker = createDeltaker(arenaDeltaker, gjennomforing)
		val deltakerData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, arenaDeltakerId)

		if (skalRetryes(deltakerData, message)) {
			throw DependencyNotIngestedException("Forrige melding på deltaker med id=$arenaDeltakerId er ikke håndtert enda")
		}

		if (skalVente(deltakerData)) {
			Thread.sleep(500)
		}
		log.info("Prosesserer melding for deltaker id=${deltaker.id} arenaId=$arenaDeltakerId op=${message.operationType} er sendt")

		if (message.operationType == AmtOperation.DELETED) {
			handleDeleteMessage(arenaDeltakerRaw, deltaker, arenaDeltakerId, message, deltakerData, gjennomforing)
		} else {
			sendMessageAndUpdateIngestStatus(message, deltaker, arenaDeltakerId, gjennomforing=gjennomforing)
			log.info("Lagrer deltaker med id=${deltaker.id}")
			deltakerRepository.upsert(arenaDeltakerRaw.toDbo())

		}
		metrics.publishMetrics(message)
		log.info("Deltaker med id=${deltaker.id} ferdig prosessert")
	}

	private fun externalDeltakerGuard(arenaDeltakerRaw: ArenaDeltaker) {
		val arenaDeltakerId = arenaDeltakerRaw.TILTAKDELTAKER_ID.toString()
		if (!arenaDeltakerRaw.EKSTERN_ID.isNullOrEmpty()) {
			val eksternId = UUID.fromString(arenaDeltakerRaw.EKSTERN_ID)

			val arenaId = arenaDataIdTranslationService.hentArenaId(eksternId)
			if (arenaId == null) {
				arenaDataIdTranslationService.opprettIdTranslation(arenaDeltakerId, eksternId)
			}
			else if (arenaId != arenaDeltakerId) {
				log.error("Fikk arenadeltaker med id $arenaDeltakerId og EKSTERN_ID ${arenaDeltakerRaw.EKSTERN_ID} men arenaId er allerede mappet til $arenaId")
				throw ValidationException("Fikk arenadeltaker med id $arenaDeltakerId og EKSTERN_ID ${arenaDeltakerRaw.EKSTERN_ID} men arenaId er allerede mappet til $arenaId")
			}

			throw ExternalSourceSystemException("Deltaker har eksternid ${arenaDeltakerRaw.EKSTERN_ID}")
		}
		if (arenaDeltakerRaw.DELTAKERTYPEKODE == "EKSTERN") {
			throw ExternalSourceSystemException("Deltaker har deltakertypekode ekstern, arenaid $arenaDeltakerId")
		}

	}
	private fun sendMessageAndUpdateIngestStatus(
		message: ArenaDeltakerKafkaMessage,
		deltaker: AmtDeltaker,
		arenaDeltakerId: String,
		operation: AmtOperation = message.operationType,
		gjennomforing: Gjennomforing? = null,
	) {
		val deltakerKafkaMessage = AmtKafkaMessageDto(
			type = PayloadType.DELTAKER,
			operation = operation,
			payload = deltaker
		)
		if(gjennomforing?.tiltakstype?.arenaKode in listOf("ENKELAMO","ENKFAGYRKE", "HOYEREUTD")) {
			kafkaProducerService.produceEnkeltplassDeltaker(deltaker.id,deltakerKafkaMessage)
		}
		else {
			kafkaProducerService.sendTilAmtTiltak(deltaker.id, deltakerKafkaMessage)
		}
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaDeltakerId))
		log.info("Melding for deltaker id=${deltaker.id} arenaId=$arenaDeltakerId transactionId=${deltakerKafkaMessage.transactionId} op=${deltakerKafkaMessage.operation} er sendt")
	}

	private fun skalVente(deltakerData: List<ArenaDataDbo>): Boolean {
		// Når flere meldinger for samme deltaker sendes så raskt samtidig til amt-tiltak og andre
		// så øker det sjansen for at en eller flere race-condtions inntreffer...
		val sisteMelding = deltakerData.findLast { it.ingestStatus == IngestStatus.HANDLED }
		return sisteMelding
			?.ingestedTimestamp
			?.isAfter(LocalDateTime.now().minus(Duration.ofMillis(500))) == true
	}

	private fun skalRetryes(
		deltakerData: List<ArenaDataDbo>,
		message: ArenaDeltakerKafkaMessage,
	): Boolean {
		// Hvis det finnes en eldre melding på deltaker som ikke er håndtert så skal meldingen få status RETRY
		val eldreMeldingVenter = deltakerData
			.filter { it.operationPosition < message.operationPosition }
			.firstOrNull { it.ingestStatus in listOf(IngestStatus.RETRY, IngestStatus.FAILED, IngestStatus.WAITING) }
		return eldreMeldingVenter != null
	}

	private fun handleDeleteMessage(
		arenaDeltakerRaw: ArenaDeltaker,
		amtDeltaker: AmtDeltaker,
		arenaDeltakerId: String,
		message: ArenaDeltakerKafkaMessage,
		deltakerData: List<ArenaDataDbo>,
		gjennomforing: Gjennomforing
	) {
		// Hvis deltakeren er flyttet til hist-tabellen betyr det at deltakeren deltar på samme gjennomføring flere ganger,
		// og den eldre deltakelsen skal ikke slettes.
		val arenaHistId = arenaDataIdTranslationService.hentArenaHistId(amtDeltaker.id)
		if (arenaHistId != null) {
			log.info("Mottatt delete-melding for deltaker id=${amtDeltaker.id} arenaId=$arenaDeltakerId, blir ikke behandlet fordi det finnes hist-melding på samme deltaker")
			arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaDeltakerId, "Slettes ikke fordi deltaker ble historisert"))
			deltakerRepository.upsert(arenaDeltakerRaw.toDbo()) // denne kan fjernes når vi har kjørt igjennom delete meldinger
		} else {
			if (getRetryAttempts(deltakerData, message) < 2) {
				log.info("Fant ikke hist-deltaker for deltaker id=${amtDeltaker.id} arenaId=$arenaDeltakerId, venter litt..")
				throw DependencyNotIngestedException("Fant ikke hist-deltaker for deltaker id=${amtDeltaker.id} arenaId=$arenaDeltakerId")
			} else {
				log.info("Sender tombstone for deltaker id=${amtDeltaker.id} arenaId=$arenaDeltakerId")
				sendMessageAndUpdateIngestStatus(
					message,
					amtDeltaker.toFeilregistrertDeltaker(),
					arenaDeltakerId,
					AmtOperation.MODIFIED,
					gjennomforing = gjennomforing,
				)
			}
		}
	}

	private fun getRetryAttempts(
		deltakerData: List<ArenaDataDbo>,
		message: ArenaDeltakerKafkaMessage,
	): Int {
		return deltakerData.find { it.operationPosition == message.operationPosition }?.ingestAttempts ?: 0
	}

	fun createDeltaker(arenaDeltaker: TiltakDeltaker, gjennomforing: Gjennomforing, erHistDeltaker: Boolean = false): AmtDeltaker {
		val personIdent = ordsClient.hentFnr(arenaDeltaker.personId)
			?: throw ValidationException("Arena mangler personlig ident for personId=${arenaDeltaker.personId}")

		val deltakerId = arenaDataIdTranslationService.hentEllerOpprettNyDeltakerId(arenaDeltaker.tiltakdeltakerId, erHistDeltaker)

		return arenaDeltaker.constructDeltaker(
			amtDeltakerId = deltakerId,
			gjennomforingId = gjennomforing.id,
			gjennomforingSluttDato = gjennomforing.sluttDato,
			erGjennomforingAvsluttet = gjennomforing.erAvsluttet(),
			erKurs = gjennomforing.erKurs(),
			personIdent = personIdent,
		)
	}

	fun getGjennomforing(arenaGjennomforingId: String): Gjennomforing {
		val gjennomforing = gjennomforingService.get(arenaGjennomforingId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		if (!gjennomforing.isSupported) {
			throw IgnoredException("Deltaker på gjennomføring med arenakode $arenaGjennomforingId er ikke støttet")
		} else if (!gjennomforing.isValid) {
			throw DependencyNotValidException("Deltaker på ugyldig gjennomføring <$arenaGjennomforingId>")
		}

		// id kan være null for våre typer fordi id ikke ble lagret fra starten
		// og pga en bug se trellokort #877
		val gjennomforingId = gjennomforing.id?: getGjennomforingId(gjennomforing.arenaId).also {
			gjennomforingService.setGjennomforingId(gjennomforing.arenaId, it)
		}

		return mulighetsrommetApiClient.hentGjennomforingV2(gjennomforingId)
	}

	private fun getGjennomforingId(arenaId: String): UUID {
		return mulighetsrommetApiClient.hentGjennomforingId(arenaId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=${arenaId} skal bli håndtert av Mulighetsrommet")
	}

}

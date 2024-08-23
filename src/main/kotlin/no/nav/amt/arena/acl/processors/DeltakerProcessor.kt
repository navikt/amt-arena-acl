package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.MulighetsrommetApiClient
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.DependencyNotValidException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.metrics.DeltakerMetricHandler
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
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
open class DeltakerProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingService: GjennomforingService,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val metrics: DeltakerMetricHandler,
	private val kafkaProducerService: KafkaProducerService,
	private val mulighetsrommetApiClient: MulighetsrommetApiClient
) : ArenaMessageProcessor<ArenaDeltakerKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltakerRaw = message.getData()
		val arenaDeltakerId = arenaDeltakerRaw.TILTAKDELTAKER_ID.toString()
		val arenaGjennomforingId = arenaDeltakerRaw.TILTAKGJENNOMFORING_ID.toString()

		if (!arenaDeltakerRaw.EKSTERN_ID.isNullOrEmpty()) {
			throw IgnoredException("Ignorerer deltaker som har eksternid ${arenaDeltakerRaw.EKSTERN_ID}")
		}

		val gjennomforing = getGjennomforing(arenaGjennomforingId)
		val deltaker = createDeltaker(arenaDeltakerRaw, gjennomforing)

		val deltakerData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, arenaDeltakerId)

		if (skalRetryes(deltakerData, message)) {
			throw DependencyNotIngestedException("Forrige melding på deltaker med id=$arenaDeltakerId er ikke håndtert enda")
		}

		if (skalVente(deltakerData)) {
			Thread.sleep(500)
		}

		val deltakerKafkaMessage = AmtKafkaMessageDto(
			type = PayloadType.DELTAKER,
			operation = message.operationType,
			payload = deltaker
		)

		kafkaProducerService.sendTilAmtTiltak(deltaker.id, deltakerKafkaMessage)
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaDeltakerId))

		log.info("Melding for deltaker id=${deltaker.id} arenaId=$arenaDeltakerId transactionId=${deltakerKafkaMessage.transactionId} op=${deltakerKafkaMessage.operation} er sendt")
		metrics.publishMetrics(message)
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

	// skal gjøres private igjen etter engangsjobb
	fun createDeltaker(arenaDeltakerRaw: ArenaDeltaker, gjennomforing: Gjennomforing): AmtDeltaker {
		val arenaDeltaker = arenaDeltakerRaw
			.tryRun { it.mapTiltakDeltaker() }
			.getOrThrow()

		val personIdent = ordsClient.hentFnr(arenaDeltaker.personId)
			?: throw ValidationException("Arena mangler personlig ident for personId=${arenaDeltaker.personId}")

		val deltakerAmtId = arenaDataIdTranslationService.hentEllerOpprettNyDeltakerId(arenaDeltaker.tiltakdeltakerId)

		return arenaDeltaker.constructDeltaker(
			amtDeltakerId = deltakerAmtId,
			gjennomforingId = gjennomforing.id,
			gjennomforingSluttDato = gjennomforing.sluttDato,
			erGjennomforingAvsluttet = gjennomforing.erAvsluttet(),
			erKurs = gjennomforing.erKurs(),
			personIdent = personIdent,
		)
	}

	// skal gjøres private igjen etter engangsjobb
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

		return mulighetsrommetApiClient.hentGjennomforing(gjennomforingId)
	}

	private fun getGjennomforingId(arenaId: String): UUID {
		return mulighetsrommetApiClient.hentGjennomforingId(arenaId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=${arenaId} skal bli håndtert av Mulighetsrommet")
	}

}

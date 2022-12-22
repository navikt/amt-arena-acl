package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.clients.mr_arena_adapter.MrArenaAdapterClient
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.metrics.DeltakerMetricHandler
import no.nav.amt.arena.acl.processors.converters.GjennomforingStatusConverter
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.services.ToggleService
import no.nav.amt.arena.acl.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
open class DeltakerProcessor(
	val meterRegistry: MeterRegistry,
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingService: GjennomforingService,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val metrics: DeltakerMetricHandler,
	private val kafkaProducerService: KafkaProducerService,
	private val mrArenaAdapterClient: MrArenaAdapterClient,
	private val toggleService: ToggleService,
) : ArenaMessageProcessor<ArenaDeltakerKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltakerRaw = message.getData()
		val arenaGjennomforingId = arenaDeltakerRaw.TILTAKGJENNOMFORING_ID.toString()

		val gjennomforing = if (toggleService.hentGjennomforingFraMulighetsrommetEnabled()) {
			getGjennomforingFraMulighetsrommet(arenaGjennomforingId)
		} else {
			getGjennomforing(arenaGjennomforingId)
		}

		val arenaDeltaker = arenaDeltakerRaw.mapTiltakDeltaker()

		val personIdent = ordsClient.hentFnr(arenaDeltaker.personId)
			?: throw IllegalStateException("Expected person with personId=${arenaDeltaker.personId} to exist")

		val deltakerAmtId = arenaDataIdTranslationService.hentEllerOpprettNyDeltakerId(arenaDeltaker.tiltakdeltakerId)

		val amtDeltaker = arenaDeltaker.constructDeltaker(
			amtDeltakerId = deltakerAmtId,
			gjennomforingId = gjennomforing.id,
			gjennomforingStatus = gjennomforing.status,
			personIdent = personIdent
		)

		meterRegistry.counter(
			"amt.arena-acl.deltaker.status",
			listOf(Tag.of("arena", arenaDeltaker.deltakerStatusKode.name), Tag.of("amt-tiltak", arenaDeltaker.deltakerStatusKode.name))
		).increment()

		val amtData = AmtKafkaMessageDto(
			type = PayloadType.DELTAKER,
			operation = message.operationType,
			payload = amtDeltaker
		)

		kafkaProducerService.sendTilAmtTiltak(amtDeltaker.id, amtData)

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaDeltaker.tiltakdeltakerId))

		secureLog.info("Melding for deltaker id=$deltakerAmtId arenaId=${arenaDeltaker.tiltakdeltakerId} personId=${arenaDeltaker.personId} fnr=$personIdent er sendt")
		log.info("Melding for deltaker id=$deltakerAmtId arenaId=${arenaDeltaker.tiltakdeltakerId} transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
		metrics.publishMetrics(message)
	}

	private fun getGjennomforing(arenaGjennomforingId: String): GjennomforingInfo {
		val gjennomforingId = arenaDataIdTranslationService.findGjennomforingIdTranslation(arenaGjennomforingId)?.amtId
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		if (gjennomforingService.isIgnored(gjennomforingId)) {
			throw IgnoredException("Deltaker på en gjennomføring $gjennomforingId er ignorert")
		}

		val gjennomforingInfo = gjennomforingService.getGjennomforing(gjennomforingId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		return GjennomforingInfo(gjennomforingInfo.id, gjennomforingInfo.status)
	}

	private fun getGjennomforingFraMulighetsrommet(arenaGjennomforingId: String): GjennomforingInfo {
		val gjennomforingId = mrArenaAdapterClient.hentGjennomforingId(arenaGjennomforingId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert av Mulighetsrommet")

		val gjennomforing = mrArenaAdapterClient.hentGjennomforing(gjennomforingId)

		if (gjennomforingService.isSupportedTiltak(gjennomforing.tiltak.arenaKode)) {
			throw IgnoredException("Deltaker på gjennomføring $gjennomforingId er ikke støttet")
		}

		val gjennomforingArenaData = mrArenaAdapterClient.hentGjennomforingArenaData(gjennomforingId)
		val status = GjennomforingStatusConverter.convert(gjennomforingArenaData.status)

		return GjennomforingInfo(gjennomforing.id, status)
	}

	data class GjennomforingInfo(
		val id: UUID,
		val status: AmtGjennomforing.Status,
	)

}

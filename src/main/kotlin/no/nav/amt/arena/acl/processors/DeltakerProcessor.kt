package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.metrics.DeltakerMetricHandler
import no.nav.amt.arena.acl.processors.converters.ArenaDeltakerStatusConverter
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.ArenaGjennomforingDbo
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class DeltakerProcessor(
	val meterRegistry: MeterRegistry,
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingService: GjennomforingService,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val metrics: DeltakerMetricHandler,
	private val kafkaProducerService: KafkaProducerService
) : ArenaMessageProcessor<ArenaDeltakerKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltaker = message.getData()
		val gjennomforing = getGjennomforing(arenaDeltaker.TILTAKGJENNOMFORING_ID.toString())

		val deltaker = arenaDeltaker.mapTiltakDeltaker()

		val personIdent = ordsClient.hentFnr(deltaker.personId)
			?: throw IllegalStateException("Expected person with personId=${deltaker.personId} to exist")

		val deltakerAmtId = arenaDataIdTranslationService.hentEllerOpprettNyDeltakerId(deltaker.tiltakdeltakerId)

		val amtDeltaker = deltaker.toAmtDeltaker(
			amtDeltakerId = deltakerAmtId,
			gjennomforingId = gjennomforing.id,
			personIdent = personIdent
		)

		meterRegistry.counter(
			"amt.arena-acl.deltaker.status",
			listOf(Tag.of("arena", deltaker.deltakerStatusKode.name), Tag.of("amt-tiltak", deltaker.deltakerStatusKode.name))
		).increment()

		val amtData = AmtKafkaMessageDto(
			type = PayloadType.DELTAKER,
			operation = message.operationType,
			payload = amtDeltaker
		)

		kafkaProducerService.sendTilAmtTiltak(amtDeltaker.id, amtData)

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(deltaker.tiltakdeltakerId))

		secureLog.info("Melding for deltaker id=$deltakerAmtId arenaId=${deltaker.tiltakdeltakerId} personId=${deltaker.personId} fnr=$personIdent er sendt")
		log.info("Melding for deltaker id=$deltakerAmtId arenaId=${deltaker.tiltakdeltakerId} transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
		metrics.publishMetrics(message)
	}

	private fun getGjennomforing(arenaGjennomforingId: String): ArenaGjennomforingDbo {
		val gjennomforingId = arenaDataIdTranslationService.findGjennomforingIdTranslation(arenaGjennomforingId)?.amtId
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		if (gjennomforingService.isIgnored(gjennomforingId)) {
			throw IgnoredException("Deltaker på en gjennomføring $gjennomforingId er ignorert")
		}

		val gjennomforingInfo = gjennomforingService.getGjennomforing(gjennomforingId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		return gjennomforingInfo
	}

	private fun TiltakDeltaker.toAmtDeltaker(
		amtDeltakerId: UUID,
		gjennomforingId: UUID,
		personIdent: String
	): AmtDeltaker {
		val converter = ArenaDeltakerStatusConverter(
			deltakerStatusKode = deltakerStatusKode,
			deltakerRegistrertDato = regDato,
			startDato = datoFra,
			sluttDato = datoTil,
			datoStatusEndring = datoStatusendring?.toLocalDate(),
		)

		return AmtDeltaker(
			id = amtDeltakerId,
			gjennomforingId = gjennomforingId,
			personIdent = personIdent,
			startDato = datoFra,
			sluttDato = datoTil,
			status = converter.getStatus(),
			dagerPerUke = dagerPerUke,
			prosentDeltid = prosentDeltid,
			registrertDato = regDato,
			statusEndretDato = converter.getEndretDato(),
			innsokBegrunnelse = innsokBegrunnelse
		)
	}

}

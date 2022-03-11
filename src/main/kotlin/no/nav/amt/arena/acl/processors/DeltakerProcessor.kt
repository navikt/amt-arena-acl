package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.micrometer.core.instrument.MeterRegistry
import no.nav.amt.arena.acl.domain.db.toUpsertWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.metrics.DeltakerMetricHandler
import no.nav.amt.arena.acl.processors.converters.DeltakerEndretDatoConverter
import no.nav.amt.arena.acl.processors.converters.DeltakerStatusConverter
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class DeltakerProcessor(
	val meterRegistry: MeterRegistry,
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val metrics: DeltakerMetricHandler,
	private val kafkaProducerService: KafkaProducerService
) : ArenaMessageProcessor<ArenaDeltakerKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)
	private val statusConverter = DeltakerStatusConverter(meterRegistry)
	private val statusEndretDatoConverter = DeltakerEndretDatoConverter()

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltaker: TiltakDeltaker = message.getData().mapTiltakDeltaker()

		val gjennomforingInfo =
			arenaDataIdTranslationService.findGjennomforingIdTranslation(arenaDeltaker.tiltakgjennomforingId)
				?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=${arenaDeltaker.tiltakgjennomforingId} skal bli håndtert")

		if (gjennomforingInfo.ignored) {
			throw IgnoredException("Ikke støttet tiltak")
		}

		val personIdent = ordsClient.hentFnr(arenaDeltaker.personId)
			?: throw IllegalStateException("Expected person with personId=${arenaDeltaker.personId} to exist")

		val deltakerAmtId = arenaDataIdTranslationService.hentEllerOpprettNyDeltakerId(arenaDeltaker.tiltakdeltakerId)

		val amtDeltaker = arenaDeltaker.toAmtDeltaker(
			amtDeltakerId = deltakerAmtId,
			gjennomforingId = gjennomforingInfo.amtId,
			personIdent = personIdent
		)

		arenaDataIdTranslationService.upsertDeltakerIdTranslation(
			deltakerArenaId = arenaDeltaker.tiltakdeltakerId,
			deltakerAmtId = deltakerAmtId,
			ignored = false
		)

		val amtData = AmtKafkaMessageDto(
			type = PayloadType.DELTAKER,
			operation = message.operationType,
			payload = arenaDeltaker.toAmtDeltaker(deltakerAmtId, gjennomforingInfo.amtId, personIdent)
		)

		kafkaProducerService.sendTilAmtTiltak(amtDeltaker.id, amtData)

		arenaDataRepository.upsert(message.toUpsertWithStatusHandled(arenaDeltaker.tiltakdeltakerId))

		secureLog.info("Melding for deltaker id=$deltakerAmtId arenaId=${arenaDeltaker.tiltakdeltakerId} personId=${arenaDeltaker.personId} fnr=$personIdent er sendt")
		log.info("Melding for deltaker id=$deltakerAmtId arenaId=${arenaDeltaker.tiltakdeltakerId} transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
		metrics.publishMetrics(message)
	}

	private fun TiltakDeltaker.toAmtDeltaker(
		amtDeltakerId: UUID,
		gjennomforingId: UUID,
		personIdent: String
	): AmtDeltaker {

		return AmtDeltaker(
			id = amtDeltakerId,
			gjennomforingId = gjennomforingId,
			personIdent = personIdent,
			startDato = datoFra,
			sluttDato = datoTil,
			status = statusConverter.convert(
				deltakerStatusCode = deltakerStatusKode,
				deltakerRegistrertDato = regDato,
				startDato = datoFra,
				sluttDato = datoTil,
				datoStatusEndring = datoStatusendring
			),
			dagerPerUke = dagerPerUke,
			prosentDeltid = prosentDeltid,
			registrertDato = regDato,
			statusEndretDato = statusEndretDatoConverter.convert(
				deltakerStatus = deltakerStatusKode,
				datoStatusEndring = datoStatusendring?.atStartOfDay(),
				oppstartDato = datoFra?.atStartOfDay(),
				sluttDato = datoTil?.atStartOfDay()
			)
		)
	}

}

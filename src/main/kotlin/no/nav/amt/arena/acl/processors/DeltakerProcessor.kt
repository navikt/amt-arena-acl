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

	override fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltaker = message.getData()
		val arenaGjennomforingId = arenaDeltaker.TILTAKGJENNOMFORING_ID.toString()

		if (harStatusSomSkalIgnoreres(arenaDeltaker.DELTAKERSTATUSKODE)) {
			throw IgnoredException("Deltakeren har status=${arenaDeltaker.DELTAKERSTATUSKODE} som ikke skal håndteres")
		}

		val gjennomforingInfo =
			arenaDataIdTranslationService.findGjennomforingIdTranslation(arenaGjennomforingId)
				?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		if (gjennomforingInfo.ignored) {
			throw IgnoredException("Er deltaker på en gjennomførig som ikke er støttet")
		}

		val deltaker = arenaDeltaker.mapTiltakDeltaker()

		val personIdent = ordsClient.hentFnr(deltaker.personId)
			?: throw IllegalStateException("Expected person with personId=${deltaker.personId} to exist")

		val deltakerAmtId = arenaDataIdTranslationService.hentEllerOpprettNyDeltakerId(deltaker.tiltakdeltakerId)

		val amtDeltaker = deltaker.toAmtDeltaker(
			amtDeltakerId = deltakerAmtId,
			gjennomforingId = gjennomforingInfo.amtId,
			personIdent = personIdent
		)

		meterRegistry.counter(
				"amt.arena-acl.deltaker.status",
				listOf(Tag.of("arena", deltaker.deltakerStatusKode), Tag.of("amt-tiltak", deltaker.deltakerStatusKode))
			).increment()

		arenaDataIdTranslationService.upsertDeltakerIdTranslation(
			deltakerArenaId = deltaker.tiltakdeltakerId,
			deltakerAmtId = deltakerAmtId,
			ignored = false
		)

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

	private fun harStatusSomSkalIgnoreres(arenaDeltakerStatusKode: String): Boolean {
		val statuserSomIgnoreres = listOf("VENTELISTE", "AKTUELL", "JATAKK", "INFOMOETE")
		return statuserSomIgnoreres.contains(arenaDeltakerStatusKode)
	}

}

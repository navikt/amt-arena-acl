package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.micrometer.core.instrument.MeterRegistry
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaTiltakDeltaker
import no.nav.amt.arena.acl.domain.arena.TiltakDeltaker
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.metrics.DeltakerMetricHandler
import no.nav.amt.arena.acl.processors.converters.DeltakerEndretDatoConverter
import no.nav.amt.arena.acl.processors.converters.DeltakerStatusConverter
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.utils.SecureLog.secureLog
import no.nav.common.kafka.producer.KafkaProducerClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class DeltakerProcessor(
	repository: ArenaDataRepository,
	val meterRegistry: MeterRegistry,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val metrics: DeltakerMetricHandler,
	kafkaProducer: KafkaProducerClient<String, String>
) : AbstractArenaProcessor<ArenaTiltakDeltaker>(
	repository = repository,
	meterRegistry = meterRegistry,
	clazz = ArenaTiltakDeltaker::class.java,
	kafkaProducer = kafkaProducer
) {

	private val log = LoggerFactory.getLogger(javaClass)
	private val statusConverter = DeltakerStatusConverter(meterRegistry)
	private val statusEndretDatoConverter = DeltakerEndretDatoConverter()

	override fun handleEntry(data: ArenaData) {
		val arenaDeltaker: TiltakDeltaker = data.getMainObject<ArenaTiltakDeltaker>().mapTiltakDeltaker()

		val gjennomforingInfo =
			arenaDataIdTranslationService.findGjennomforingIdTranslation(arenaDeltaker.tiltakgjennomforingId)
				?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=${arenaDeltaker.tiltakgjennomforingId} skal bli håndtert")

		if (gjennomforingInfo.ignored) {
			throw IgnoredException("Ikke støttet tiltak")
		}

		val personIdent = ordsClient.hentFnr(arenaDeltaker.personId)
			?: throw IllegalStateException("Expected person with personId=${arenaDeltaker.personId} to exist")

		val deltakerAmtId = arenaDataIdTranslationService.hentEllerOpprettNyDeltakerId(data.arenaId)

		val amtDeltaker = arenaDeltaker.toAmtDeltaker(
			amtDeltakerId = deltakerAmtId,
			gjennomforingId = gjennomforingInfo.amtId,
			personIdent = personIdent
		)

		arenaDataIdTranslationService.upsertDeltakerIdTranslation(
			deltakerArenaId = data.arenaId,
			deltakerAmtId = deltakerAmtId,
			ignored = false
		)

		val amtData = AmtWrapper(
			type = "DELTAKER",
			operation = data.operation,
			payload = arenaDeltaker.toAmtDeltaker(deltakerAmtId, gjennomforingInfo.amtId, personIdent)
		)

		send(amtDeltaker.id, objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsHandled())

		secureLog.info("Melding for deltaker id=$deltakerAmtId arenaId=${arenaDeltaker.tiltakdeltakerId} personId=${arenaDeltaker.personId} fnr=$personIdent er sendt")
		log.info("Melding for deltaker id=$deltakerAmtId arenaId=${arenaDeltaker.tiltakdeltakerId} transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
		metrics.publishMetrics(data)
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

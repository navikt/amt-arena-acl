package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.micrometer.core.instrument.MeterRegistry
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaTiltakGjennomforing
import no.nav.amt.arena.acl.domain.arena.TiltakGjennomforing
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.processors.converters.GjennomforingStatusConverter
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class TiltakGjennomforingProcessor(
	repository: ArenaDataRepository,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val tiltakRepository: TiltakRepository,
	private val ordsClient: ArenaOrdsProxyClient,
	meterRegistry: MeterRegistry,
	kafkaProducer: KafkaProducerClientImpl<String, String>
) : AbstractArenaProcessor<ArenaTiltakGjennomforing>(
	repository = repository,
	meterRegistry = meterRegistry,
	clazz = ArenaTiltakGjennomforing::class.java,
	kafkaProducer = kafkaProducer
) {

	private val log = LoggerFactory.getLogger(javaClass)
	private val statusConverter = GjennomforingStatusConverter()

	override fun handleEntry(data: ArenaData) {
		val arenaTiltakGjennomforing = data.getMainObject<ArenaTiltakGjennomforing>()

		val gjennomforingId = arenaDataIdTranslationService.hentEllerOpprettNyGjennomforingId(data.arenaId)

		if (!isSupportedTiltak(arenaTiltakGjennomforing.TILTAKSKODE)) {
			arenaDataIdTranslationService.upsertGjennomforingIdTranslation(
				gjennomforingArenaId = data.arenaId,
				gjennomforingAmtId = gjennomforingId,
				ignored = true
			)

			throw IgnoredException("${arenaTiltakGjennomforing.TILTAKSKODE} er ikke et støttet tiltak")
		}

		val arenaGjennomforing = arenaTiltakGjennomforing.mapTiltakGjennomforing()

		val tiltak = tiltakRepository.getByKode(arenaGjennomforing.tiltakskode)
			?: throw DependencyNotIngestedException("Venter på at tiltaket med koden=${arenaGjennomforing.tiltakskode} skal bli håndtert")

		val virksomhetsnummer = ordsClient.hentVirksomhetsnummer(arenaGjennomforing.arbgivIdArrangor)

		val amtGjennomforing = arenaGjennomforing.toAmtGjennomforing(
			amtTiltak = tiltak,
			amtGjennomforingId = gjennomforingId,
			virksomhetsnummer = virksomhetsnummer
		)

		arenaDataIdTranslationService.upsertGjennomforingIdTranslation(
			gjennomforingArenaId = data.arenaId,
			gjennomforingAmtId = gjennomforingId,
			ignored = false
		)

		val amtData = AmtWrapper(
			type = "GJENNOMFORING",
			operation = data.operation,
			payload = arenaGjennomforing.toAmtGjennomforing(tiltak, gjennomforingId, virksomhetsnummer)
		)

		send(amtGjennomforing.id, objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsHandled())
		log.info("Melding for gjennomføring id=$gjennomforingId arenaId=${arenaGjennomforing.tiltakgjennomforingId} transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
	}

	private fun TiltakGjennomforing.toAmtGjennomforing(
		amtTiltak: AmtTiltak,
		amtGjennomforingId: UUID,
		virksomhetsnummer: String
	): AmtGjennomforing {

		return AmtGjennomforing(
			id = amtGjennomforingId,
			tiltak = amtTiltak,
			virksomhetsnummer = virksomhetsnummer,
			navn = lokaltNavn,
			startDato = datoFra,
			sluttDato = datoTil,
			registrertDato = regDato,
			fremmoteDato = datoFremmote,
			status = statusConverter.convert(tiltakstatusKode)
		)
	}
}

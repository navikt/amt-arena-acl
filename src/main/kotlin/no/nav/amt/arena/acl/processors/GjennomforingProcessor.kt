package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.domain.db.toUpsertWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakGjennomforing
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.processors.converters.GjennomforingStatusConverter
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.services.TiltakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class GjennomforingProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val tiltakService: TiltakService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val kafkaProducerService: KafkaProducerService
) : ArenaMessageProcessor<ArenaGjennomforingKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)
	private val statusConverter = GjennomforingStatusConverter()

	private val SUPPORTED_TILTAK = setOf(
		"INDOPPFAG",
	)

	private fun isSupportedTiltak(tiltakskode: String): Boolean {
		return SUPPORTED_TILTAK.contains(tiltakskode)
	}

	override fun handleArenaMessage(message: ArenaGjennomforingKafkaMessage) {
		val arenaGjennomforing = message.getData()
		val arenaGjennomforingTiltakskode = arenaGjennomforing.TILTAKSKODE
		val arenaGjennomforingId = arenaGjennomforing.TILTAKGJENNOMFORING_ID.toString()

		val gjennomforingId = arenaDataIdTranslationService.hentEllerOpprettNyGjennomforingId(arenaGjennomforingId)

		if (!isSupportedTiltak(arenaGjennomforingTiltakskode)) {
			arenaDataIdTranslationService.upsertGjennomforingIdTranslation(
				gjennomforingArenaId = arenaGjennomforingId,
				gjennomforingAmtId = gjennomforingId,
				ignored = true
			)

			throw IgnoredException("$arenaGjennomforingTiltakskode er ikke et støttet tiltak")
		}

		val gjennomforing = arenaGjennomforing.mapTiltakGjennomforing()

		val tiltak = tiltakService.getByKode(arenaGjennomforingTiltakskode)
			?: throw DependencyNotIngestedException("Venter på at tiltaket med koden=$arenaGjennomforingTiltakskode skal bli håndtert")

		val virksomhetsnummer = ordsClient.hentVirksomhetsnummer(gjennomforing.arbgivIdArrangor)

		val amtGjennomforing = gjennomforing.toAmtGjennomforing(
			amtTiltak = tiltak,
			amtGjennomforingId = gjennomforingId,
			virksomhetsnummer = virksomhetsnummer
		)

		arenaDataIdTranslationService.upsertGjennomforingIdTranslation(
			gjennomforingArenaId = gjennomforing.tiltakgjennomforingId,
			gjennomforingAmtId = gjennomforingId,
			ignored = false
		)

		val amtData = AmtKafkaMessageDto(
			type = PayloadType.GJENNOMFORING,
			operation = message.operationType,
			payload = amtGjennomforing
		)

		kafkaProducerService.sendTilAmtTiltak(amtGjennomforing.id, amtData)
		arenaDataRepository.upsert(message.toUpsertWithStatusHandled(gjennomforing.tiltakgjennomforingId))
		log.info("Melding for gjennomføring id=$gjennomforingId arenaId=${gjennomforing.tiltakgjennomforingId} transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
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

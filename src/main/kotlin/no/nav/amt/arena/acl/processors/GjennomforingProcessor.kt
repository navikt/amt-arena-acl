package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforing
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakGjennomforing
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.processors.converters.GjennomforingStatusConverter
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.ArenaSakRepository
import no.nav.amt.arena.acl.services.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class GjennomforingProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaSakRepository: ArenaSakRepository,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val gjennomforingService: GjennomforingService,
	private val tiltakService: TiltakService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val kafkaProducerService: KafkaProducerService,
	private val toggleService: ToggleService,
) : ArenaMessageProcessor<ArenaGjennomforingKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaGjennomforingKafkaMessage) {
		val arenaGjennomforing = message.getData()
		val arenaGjennomforingTiltakskode = arenaGjennomforing.TILTAKSKODE
		val arenaGjennomforingId = arenaGjennomforing.TILTAKGJENNOMFORING_ID.toString()
		val gjennomforingId = arenaDataIdTranslationService.hentEllerOpprettNyGjennomforingId(arenaGjennomforingId)

		gjennomforingService.upsert(arenaGjennomforingId, arenaGjennomforingTiltakskode, isValid(arenaGjennomforing))

		if (!gjennomforingService.isSupportedTiltak(arenaGjennomforingTiltakskode)) {
			gjennomforingService.ignore(gjennomforingId)
			throw IgnoredException("$arenaGjennomforingTiltakskode er ikke et støttet tiltak")
		}

		if (toggleService.hentGjennomforingFraMulighetsrommetEnabled()) {
			val note = "Hoppet over gjennomføring $arenaGjennomforingId fordi MR-toggle er enabled"
			log.info(note)
			arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaGjennomforingId, note))
			return
		}

		val gjennomforing = arenaGjennomforing.mapTiltakGjennomforing()

		val tiltak = tiltakService.getByKode(arenaGjennomforingTiltakskode)
			?: throw DependencyNotIngestedException("Venter på at tiltaket med koden=$arenaGjennomforingTiltakskode skal bli håndtert")

		val virksomhetsnummer = ordsClient.hentVirksomhetsnummer(gjennomforing.arbgivIdArrangor)
		val sak = arenaSakRepository.hentSakMedArenaId(gjennomforing.sakId)

		val amtGjennomforing = gjennomforing.toAmtGjennomforing(
			amtTiltak = tiltak,
			amtGjennomforingId = gjennomforingId,
			virksomhetsnummer = virksomhetsnummer,
			ansvarligNavEnhetId = sak?.ansvarligEnhetId,
			sakAar = sak?.aar,
			sakLopenr = sak?.lopenr,
		)

		gjennomforingService.upsert(amtGjennomforing.toInsertDbo(gjennomforing.sakId))

		if (sak == null) throw DependencyNotIngestedException("Venter på at sak med id: ${gjennomforing.sakId} skal bli håndtert")

		val amtData = AmtKafkaMessageDto(
			type = PayloadType.GJENNOMFORING,
			operation = message.operationType,
			payload = amtGjennomforing
		)

		kafkaProducerService.sendTilAmtTiltak(amtGjennomforing.id, amtData)
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(gjennomforing.tiltakgjennomforingId))
		log.info("Melding for gjennomføring id=$gjennomforingId arenaId=${gjennomforing.tiltakgjennomforingId} transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
	}

	private fun isValid(arenaGjennomforing: ArenaGjennomforing): Boolean {
		try {
			arenaGjennomforing.mapTiltakGjennomforing()
			return true
		}
		catch(e: ValidationException) {
			return false
		}
	}

	private fun TiltakGjennomforing.toAmtGjennomforing(
		amtTiltak: AmtTiltak,
		amtGjennomforingId: UUID,
		virksomhetsnummer: String,
		ansvarligNavEnhetId: String?,
		sakAar: Int?,
		sakLopenr: Int?
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
			status = GjennomforingStatusConverter.convert(tiltakstatusKode),
			ansvarligNavEnhetId = ansvarligNavEnhetId,
			opprettetAar = sakAar,
			lopenr = sakLopenr,
		)
	}

}

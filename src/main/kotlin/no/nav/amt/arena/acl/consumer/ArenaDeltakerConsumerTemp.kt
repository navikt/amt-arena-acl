package no.nav.amt.arena.acl.consumer

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.MulighetsrommetApiClient
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusNew
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.TiltakDeltaker
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.DependencyNotValidException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.DeltakerRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.tryRun
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ArenaDeltakerConsumerTemp(
	private val arenaDataRepository: ArenaDataRepository,
	private val deltakerRepository: DeltakerRepository,
	private val gjennomforingService: GjennomforingService,
	private val arenaDataIdTranslationService: ArenaDataIdTranslationService,
	private val ordsClient: ArenaOrdsProxyClient,
	private val mulighetsrommetApiClient: MulighetsrommetApiClient
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun handleArenaMessage(message: ArenaDeltakerKafkaMessage) {
		val arenaDeltakerRaw = message.getData()
		val arenaDeltakerId = arenaDeltakerRaw.TILTAKDELTAKER_ID.toString()
		val arenaGjennomforingId = arenaDeltakerRaw.TILTAKGJENNOMFORING_ID.toString()
		val gjennomforing = getGjennomforing(arenaGjennomforingId)

		if (!gjennomforing.erEnkelplass()) {
			return
		}

		val arenaDeltaker = arenaDeltakerRaw
			.tryRun { it.mapTiltakDeltaker() }
			.getOrThrow()

		val deltakerData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, arenaDeltakerId)

		if (!skalLagreDeltaker(deltakerData, message)) {
			log.info("TEMP Melding for arenaId=$arenaDeltakerId pos=${message.operationPosition} op=${message.operationType} er allerede håndtert av nyere melding, hopper over lagring")
			return
		}
		val deltaker = createDeltaker(arenaDeltaker, gjennomforing)

		log.info("TEMP Lagrer ${gjennomforing.tiltakstype} deltaker med id=${deltaker.id}")
		arenaDataRepository.upsert(message.toUpsertInputWithStatusNew(arenaDeltakerId))
		deltakerRepository.upsert(arenaDeltakerRaw.toDbo())

		log.info("TEMP Deltaker med id=${deltaker.id} ferdig prosessert")
	}

	// Hvis det finnes en nyere melding på deltaker så skal ikke denne meldingen l
	private fun skalLagreDeltaker(
		deltakerData: List<ArenaDataDbo>,
		message: ArenaDeltakerKafkaMessage,
	): Boolean {
		val sisteLagredeDeltaker = deltakerData
			.maxByOrNull { it.operationPosition.toLong() }
			?: return true
		return message.operationPosition.toLong() > sisteLagredeDeltaker.operationPosition.toLong()
	}

	fun createDeltaker(
		arenaDeltaker: TiltakDeltaker,
		gjennomforing: Gjennomforing,
		erHistDeltaker: Boolean = false
	): AmtDeltaker {
		val personIdent = ordsClient.hentFnr(arenaDeltaker.personId)
			?: throw ValidationException("TEMP Arena mangler personlig ident for personId=${arenaDeltaker.personId}")

		val deltakerId =
			arenaDataIdTranslationService.hentEllerOpprettNyDeltakerId(arenaDeltaker.tiltakdeltakerId, erHistDeltaker)

		return arenaDeltaker.constructDeltaker(
			amtDeltakerId = deltakerId,
			gjennomforingId = gjennomforing.id,
			gjennomforingSluttDato = gjennomforing.sluttDato,
			erGjennomforingAvsluttet = gjennomforing.erAvsluttet(),
			deltakelseKreverGodkjenningLoep = gjennomforing.erKurs() || gjennomforing.erEnkelplass(),
			personIdent = personIdent,
		)
	}

	fun getGjennomforing(arenaGjennomforingId: String): Gjennomforing {
		val gjennomforing = gjennomforingService.get(arenaGjennomforingId)
			?: throw DependencyNotIngestedException("TEMP Venter på at gjennomføring med id=$arenaGjennomforingId skal bli håndtert")

		if (!gjennomforing.isSupported) {
			throw IgnoredException("TEMP Deltaker på gjennomføring med arenakode $arenaGjennomforingId er ikke støttet")
		} else if (!gjennomforing.isValid) {
			throw DependencyNotValidException("TEMP Deltaker på ugyldig gjennomføring <$arenaGjennomforingId>")
		}

		// id kan være null for våre typer fordi id ikke ble lagret fra starten
		// og pga en bug se trellokort #877
		val gjennomforingId = gjennomforing.id ?: getGjennomforingId(gjennomforing.arenaId).also {
			gjennomforingService.setGjennomforingId(gjennomforing.arenaId, it)
		}

		return mulighetsrommetApiClient.hentGjennomforingV2(gjennomforingId)
	}

	private fun getGjennomforingId(arenaId: String): UUID {
		return mulighetsrommetApiClient.hentGjennomforingId(arenaId)
			?: throw DependencyNotIngestedException("TEMP Venter på at gjennomføring med id=${arenaId} skal bli håndtert av Mulighetsrommet")
	}

}

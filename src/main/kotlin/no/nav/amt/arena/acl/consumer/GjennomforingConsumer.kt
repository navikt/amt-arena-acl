package no.nav.amt.arena.acl.consumer

import no.nav.amt.arena.acl.clients.mulighetsrommet_api.MulighetsrommetApiClientImpl
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.GjennomforingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GjennomforingConsumer(
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingService: GjennomforingService,
	private val mulighetsrommetApiClient: MulighetsrommetApiClientImpl,
) : ArenaMessageConsumer<ArenaGjennomforingKafkaMessage> {
	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaGjennomforingKafkaMessage) {
		val arenaGjennomforing = message.getData()
		val arenaTiltakskode = arenaGjennomforing.TILTAKSKODE
		val arenaId = arenaGjennomforing.TILTAKGJENNOMFORING_ID.toString()

		val gjennomforingResult = arenaGjennomforing.mapTiltakGjennomforing()
		val isValid = gjennomforingResult.isSuccess

		gjennomforingService.upsert(arenaId, arenaTiltakskode, isValid)

		if (!gjennomforingService.isSupportedTiltak(arenaTiltakskode)) {
			log.info("Gjennomføring $arenaId ble ignorert fordi $arenaTiltakskode er ikke støttet")
			return
		}

		kotlin.runCatching {
			val gjennomforingId = getGjennomforingId(arenaId)
			gjennomforingService.setGjennomforingId(arenaId, gjennomforingId)
		}

		if (isValid) {
			arenaDataRepository.retryDeltakereMedGjennomforingIdOgStatus(arenaId, listOf(IngestStatus.WAITING))
		}

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaId))
		log.info("Gjennomføring $arenaId er ferdig håndtert")
	}

	private fun getGjennomforingId(arenaId: String): UUID =
		mulighetsrommetApiClient.hentGjennomforingId(arenaId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=$arenaId skal bli håndtert av Mulighetsrommet")
}

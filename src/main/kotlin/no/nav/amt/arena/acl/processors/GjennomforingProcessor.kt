package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.clients.mulighetsrommet_api.MulighetsrommetApiClient
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
open class GjennomforingProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingService: GjennomforingService,
	private val mulighetsrommetApiClient: MulighetsrommetApiClient
) : ArenaMessageProcessor<ArenaGjennomforingKafkaMessage> {

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

		val gjennomforingId = getGjennomforingId(arenaId)
		gjennomforingService.setGjennomforingId(arenaId, gjennomforingId)

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaId))

		if(isValid) retryDeltakere()

		log.info("Gjennomføring $arenaId er ferdig håndtert")

	}

	fun retryDeltakere() {
		arenaDataRepository.getByIngestStatus(ARENA_DELTAKER_TABLE_NAME, IngestStatus.WAITING, 0)
			.forEach {
				arenaDataRepository.updateIngestStatus(it.arenaId.toInt(), IngestStatus.RETRY)
			}
	}

	private fun getGjennomforingId(arenaId: String): UUID {
		return mulighetsrommetApiClient.hentGjennomforingId(arenaId)
			?: throw DependencyNotIngestedException("Venter på at gjennomføring med id=${arenaId} skal bli håndtert av Mulighetsrommet")
	}
}

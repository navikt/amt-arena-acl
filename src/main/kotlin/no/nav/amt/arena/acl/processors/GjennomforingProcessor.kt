package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.*
import no.nav.amt.arena.acl.utils.tryRun
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class GjennomforingProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingService: GjennomforingService,
) : ArenaMessageProcessor<ArenaGjennomforingKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaGjennomforingKafkaMessage) {
		val arenaGjennomforing = message.getData()
		val arenaGjennomforingTiltakskode = arenaGjennomforing.TILTAKSKODE
		val arenaGjennomforingId = arenaGjennomforing.TILTAKGJENNOMFORING_ID.toString()

		val gjennomforingResult = arenaGjennomforing.mapTiltakGjennomforing()
		val isValid = gjennomforingResult.isSuccess

		arenaGjennomforing.tryRun { it.mapTiltakGjennomforing() }

		gjennomforingService.upsert(arenaGjennomforingId, arenaGjennomforingTiltakskode, isValid)

		if (!gjennomforingService.isSupportedTiltak(arenaGjennomforingTiltakskode)) {
			log.info("Gjennomføring $arenaGjennomforingId ble ignorert fordi $arenaGjennomforingTiltakskode er ikke støttet")
			return
		}

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaGjennomforingId))
		log.info("Gjennomføring $arenaGjennomforingId er ferdig håndtert")

	}
}

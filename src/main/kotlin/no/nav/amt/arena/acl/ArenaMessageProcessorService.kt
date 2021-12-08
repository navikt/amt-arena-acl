package no.nav.amt.arena.acl

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
open class ArenaMessageProcessorService(
	private val dataRepository: ArenaDataRepository,
	private val idTranslationRepository: ArenaDataIdTranslationRepository
) {

	private val tiltakTableName = "SIAMO.TILTAK"
	private val tiltakgjennomforingTableName = "SIAMO.TILTAKGJENNOMFORING"
	private val tiltakDeltakerTableName = "SIAMO.TILTAKDELTAKER"

	private val uningestedStatuses = listOf(IngestStatus.NEW, IngestStatus.RETRY)

	fun processMessages() {
		process { dataRepository.getByIngestStatusIn(tiltakTableName, uningestedStatuses) }
		process { dataRepository.getByIngestStatusIn(tiltakgjennomforingTableName, uningestedStatuses) }
		process { dataRepository.getByIngestStatusIn(tiltakDeltakerTableName, uningestedStatuses) }
	}


	private fun process(getter: () -> List<ArenaData>) {
		var messages: List<ArenaData>

		do {
			val start = Instant.now()
			messages = getter()
			//TODO: THREADS
			messages.forEach { proccessEntry(it) }
		} while (messages.isNotEmpty())
	}

	private fun proccessEntry(entry: ArenaData) {

	}

}

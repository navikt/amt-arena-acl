package no.nav.amt.arena.acl

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.processors.TiltakProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
open class ArenaMessageProcessorService(
	private val dataRepository: ArenaDataRepository,
	private val tiltakProcessor: TiltakProcessor
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	companion object {
		private const val MAX_NUMBER_THREADS = 10

		private const val TILTAK_TABLE_NAME = "SIAMO.TILTAK"
		private const val TILTAKGJENNOMFORING_TABLE_NAME = "SIAMO.TILTAKGJENNOMFORING"
		private const val TILTAK_DELTAKER_TABLE_NAME = "SIAMO.TILTAKDELTAKER"
	}


	private val uningestedStatuses = listOf(IngestStatus.NEW, IngestStatus.RETRY)

	fun processMessages() {
		process { dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, uningestedStatuses) }
//		process { dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, uningestedStatuses) }
//		process { dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, uningestedStatuses) }
	}


	private fun process(getter: () -> List<ArenaData>) {
		var messages: List<ArenaData>

		do {
			val start = Instant.now()
			messages = getter()

			messages.forEach { proccessEntry(it) }

			log(start, messages)
		} while (messages.isNotEmpty())
	}

	private fun proccessEntry(entry: ArenaData) {
		when (entry.arenaTableName) {
			TILTAK_TABLE_NAME -> tiltakProcessor.handle(entry)
		}
	}

	private fun log(start: Instant, messages: List<ArenaData>) {
		if (messages.isEmpty()) {
			return
		}
		val table = messages.first().arenaTableName
		val first = messages.first().operationPosition
		val last = messages.last().operationPosition
		val duration = Duration.between(start, Instant.now())

		logger.info("[$table]: Handled from id $first to $last in ${duration.toMillis()} ms. (${messages.size} items)")
	}


}

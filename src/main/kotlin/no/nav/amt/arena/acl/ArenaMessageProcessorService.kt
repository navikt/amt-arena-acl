package no.nav.amt.arena.acl

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.processors.TiltakProcessor
import no.nav.amt.arena.acl.processors.TiltaksgjennomforingProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.TILTAKGJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.TILTAK_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
open class ArenaMessageProcessorService(
	private val dataRepository: ArenaDataRepository,
	private val tiltakProcessor: TiltakProcessor,
	private val tiltaksgjennomforingProcessor: TiltaksgjennomforingProcessor
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	companion object {
		private const val MAX_NUMBER_THREADS = 10
	}


	private val uningestedStatuses = listOf(IngestStatus.NEW, IngestStatus.RETRY)

	fun processMessages() {
		process { dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, uningestedStatuses) }
		process { dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, uningestedStatuses) }
//		process { dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, uningestedStatuses) }
	}


	private fun process(getter: () -> List<ArenaData>) {
		var messages: List<ArenaData>

		do {
			runBlocking {
				messages = getter()
				messages.forEach {
					launch { proccessEntry(it) }
				}
			}
		} while (messages.isNotEmpty())
	}

	private fun proccessEntry(entry: ArenaData) {
		when (entry.arenaTableName) {
			TILTAK_TABLE_NAME -> tiltakProcessor.handle(entry)
			TILTAKGJENNOMFORING_TABLE_NAME -> tiltaksgjennomforingProcessor.handle(entry)
		}
	}
}

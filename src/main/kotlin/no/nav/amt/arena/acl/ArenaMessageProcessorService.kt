package no.nav.amt.arena.acl

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.processors.DeltakerProcessor
import no.nav.amt.arena.acl.processors.TiltakProcessor
import no.nav.amt.arena.acl.processors.TiltaksgjennomforingProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.TILTAKGJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.TILTAK_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.TILTAK_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
open class ArenaMessageProcessorService(
	private val dataRepository: ArenaDataRepository,
	private val tiltakProcessor: TiltakProcessor,
	private val tiltaksgjennomforingProcessor: TiltaksgjennomforingProcessor,
	private val deltakerProcessor: DeltakerProcessor
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	companion object {
		private const val WAIT_MINUTES_BASE = 2
	}

	fun processMessages() {
		process { dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, IngestStatus.NEW) }
		process { dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, IngestStatus.NEW) }
		process { dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, IngestStatus.NEW) }

		process { filterRetry(dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, IngestStatus.RETRY)) }
		process { filterRetry(dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, IngestStatus.RETRY)) }
		process { filterRetry(dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, IngestStatus.RETRY)) }
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
			TILTAK_DELTAKER_TABLE_NAME -> deltakerProcessor.handle(entry)
		}
	}

	private fun filterRetry(list: List<ArenaData>): List<ArenaData> {
		return list
			.filter { it.lastAttempted != null }
			.filter {
				it.lastAttempted!!.isBefore(
					LocalDateTime.now()
						.minusMinutes((WAIT_MINUTES_BASE + it.ingestAttempts).toLong())
				)
			}
	}

}

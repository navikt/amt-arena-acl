package no.nav.amt.arena.acl

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
import java.time.Duration
import java.time.Instant
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
		process(dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, IngestStatus.NEW))
		process(dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, IngestStatus.NEW))
		process(dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, IngestStatus.NEW))

		process(filterRetry(dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, IngestStatus.RETRY)))
		process(filterRetry(dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, IngestStatus.RETRY)))
		process(filterRetry(dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, IngestStatus.RETRY)))
	}

	fun processFailedMessages() {
		process(dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, IngestStatus.FAILED))
		process(dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, IngestStatus.FAILED))
		process(dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, IngestStatus.FAILED))
	}

	private fun process(messages: List<ArenaData>) {
		val start = Instant.now()
		messages.forEach {
			proccessEntry(it)
		}
		log(start, messages)
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

	private fun log(start: Instant, messages: List<ArenaData>) {
		if (messages.isEmpty()) {
			return
		}
		val table = messages.first().arenaTableName
		val duration = Duration.between(start, Instant.now())

		logger.info("[$table]: Handled ${messages.size} messages in ${duration.toSeconds()}.${duration.toMillisPart()} seconds.")
	}


}

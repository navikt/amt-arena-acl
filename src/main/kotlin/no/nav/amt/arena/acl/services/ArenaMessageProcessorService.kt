package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.processors.DeltakerProcessor
import no.nav.amt.arena.acl.processors.TiltakGjennomforingProcessor
import no.nav.amt.arena.acl.processors.TiltakProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

@Service
open class ArenaMessageProcessorService(
	private val dataRepository: ArenaDataRepository,
	private val tiltakProcessor: TiltakProcessor,
	private val tiltakGjennomforingProcessor: TiltakGjennomforingProcessor,
	private val deltakerProcessor: DeltakerProcessor
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	private val mapper = ObjectMapperFactory.get()

	companion object {
		private const val WAIT_MINUTES_BASE = 2
	}

	fun handleArenaGoldenGateRecord(record: ConsumerRecord<String, String>) {
		val recordValue = record.value().removeNullCharacters()
		val data = mapper.readValue(recordValue, ArenaWrapper::class.java).toArenaData()

		processEntry(data)
	}

	fun processMessages() {
		processBatch(dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, IngestStatus.NEW))
		processBatch(dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, IngestStatus.NEW))
		processBatch(dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, IngestStatus.NEW))

		processBatch(filterRetry(dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, IngestStatus.RETRY)))
		processBatch(filterRetry(dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, IngestStatus.RETRY)))
		processBatch(filterRetry(dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, IngestStatus.RETRY)))
	}

	fun processFailedMessages() {
		processBatch(dataRepository.getByIngestStatusIn(TILTAK_TABLE_NAME, IngestStatus.FAILED))
		processBatch(dataRepository.getByIngestStatusIn(TILTAKGJENNOMFORING_TABLE_NAME, IngestStatus.FAILED))
		processBatch(dataRepository.getByIngestStatusIn(TILTAK_DELTAKER_TABLE_NAME, IngestStatus.FAILED))
	}

	private fun processBatch(entries: List<ArenaData>) {
		val start = Instant.now()
		entries.forEach {
			processEntry(it)
		}
		log(start, entries)
	}

	private fun processEntry(entry: ArenaData) {
		when (entry.arenaTableName) {
			TILTAK_TABLE_NAME -> tiltakProcessor.handle(entry)
			TILTAKGJENNOMFORING_TABLE_NAME -> tiltakGjennomforingProcessor.handle(entry)
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

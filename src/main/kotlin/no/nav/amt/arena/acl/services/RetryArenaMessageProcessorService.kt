package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.processors.DeltakerProcessor
import no.nav.amt.arena.acl.processors.GjennomforingProcessor
import no.nav.amt.arena.acl.processors.SakProcessor
import no.nav.amt.arena.acl.processors.TiltakProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
open class RetryArenaMessageProcessorService(
	private val arenaDataRepository: ArenaDataRepository,
	private val tiltakProcessor: TiltakProcessor,
	private val gjennomforingProcessor: GjennomforingProcessor,
	private val deltakerProcessor: DeltakerProcessor,
	private val sakProcessor: SakProcessor,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val mapper = ObjectMapperFactory.get()

	companion object {
		private const val MAX_INGEST_ATTEMPTS = 10
	}

	fun processMessages(batchSize: Int = 500) {
		processMessagesWithStatus(IngestStatus.RETRY, batchSize)
		retryQueuedMessages()
	}

	fun processFailedMessages(batchSize: Int = 500) {
		processMessagesWithStatus(IngestStatus.FAILED, batchSize)
	}

	private fun retryQueuedMessages() {
		val tables = listOf(
			ARENA_TILTAK_TABLE_NAME,
			ARENA_SAK_TABLE_NAME,
			ARENA_GJENNOMFORING_TABLE_NAME,
			ARENA_DELTAKER_TABLE_NAME
		)

		tables.forEach { table ->
			var fromId = 0
			var batch: List<ArenaDataDbo>

			do {
				batch = arenaDataRepository.getByIngestStatus(table, IngestStatus.QUEUED, fromId)

				batch.forEach { entry ->
					if (olderUnhandledMessages(table, entry).isEmpty()) {
						process(entry)
					} else {
						log.info("Waiting for older messages to be handled for arena data entry with id ${entry.id} in $table")
					}
				}

				fromId = batch.maxOfOrNull { it.id.plus(1) } ?: Int.MAX_VALUE

			} while (batch.isNotEmpty())
		}
	}

	private fun olderUnhandledMessages(tableName: String, entry: ArenaDataDbo): List<ArenaDataDbo> =
		arenaDataRepository.getByArenaId(tableName = tableName, arenaId = entry.arenaId)
			.filter {
				listOf(
					IngestStatus.NEW,
					IngestStatus.FAILED,
					IngestStatus.RETRY
				).contains(it.ingestStatus)
			}
			.filter { it.operationTimestamp.isBefore(entry.operationTimestamp) }


	private fun processMessagesWithStatus(status: IngestStatus, batchSize: Int) {
		processMessages(ARENA_TILTAK_TABLE_NAME, status, batchSize)
		processMessages(ARENA_SAK_TABLE_NAME, status, batchSize)
		processMessages(ARENA_GJENNOMFORING_TABLE_NAME, status, batchSize)
		processMessages(ARENA_DELTAKER_TABLE_NAME, status, batchSize)
	}

	private fun processMessages(tableName: String, status: IngestStatus, batchSize: Int) {
		var fromId = 0
		var data: List<ArenaDataDbo>

		val start = Instant.now()
		var totalHandled = 0

		do {
			data = arenaDataRepository.getByIngestStatus(tableName, status, fromId, batchSize)
			data.forEach { process(it) }
			totalHandled += data.size
			fromId = data.maxOfOrNull { it.id.plus(1) } ?: Int.MAX_VALUE
		} while (data.isNotEmpty())

		val duration = Duration.between(start, Instant.now())

		if (totalHandled > 0)
			log.info("[$tableName]: Handled $totalHandled $status messages in ${duration.toSeconds()}.${duration.toMillisPart()} seconds.")
	}

	private fun process(arenaDataDbo: ArenaDataDbo) {
		try {
			when (arenaDataDbo.arenaTableName) {
				ARENA_TILTAK_TABLE_NAME -> tiltakProcessor.handleArenaMessage(toArenaKafkaMessage(arenaDataDbo))
				ARENA_GJENNOMFORING_TABLE_NAME -> gjennomforingProcessor.handleArenaMessage(
					toArenaKafkaMessage(
						arenaDataDbo
					)
				)

				ARENA_DELTAKER_TABLE_NAME -> deltakerProcessor.handleArenaMessage(toArenaKafkaMessage(arenaDataDbo))
				ARENA_SAK_TABLE_NAME -> sakProcessor.handleArenaMessage(toArenaKafkaMessage(arenaDataDbo))
			}
		} catch (e: Exception) {
			val currentIngestAttempts = arenaDataDbo.ingestAttempts + 1
			val hasReachedMaxRetries = currentIngestAttempts >= MAX_INGEST_ATTEMPTS

			if (e is IgnoredException) {
				log.info("${arenaDataDbo.id} in table ${arenaDataDbo.arenaTableName}: '${e.message}'")
				arenaDataRepository.updateIngestStatus(arenaDataDbo.id, IngestStatus.IGNORED)
			} else if (arenaDataDbo.ingestStatus == IngestStatus.RETRY && hasReachedMaxRetries) {
				arenaDataRepository.updateIngestStatus(arenaDataDbo.id, IngestStatus.FAILED)
			}
			arenaDataRepository.updateIngestAttempts(arenaDataDbo.id, currentIngestAttempts, e.message)
		}
	}

	private inline fun <reified D> toArenaKafkaMessage(arenaDataDbo: ArenaDataDbo): ArenaKafkaMessage<D> {
		return ArenaKafkaMessage(
			arenaTableName = arenaDataDbo.arenaTableName,
			operationType = arenaDataDbo.operation,
			operationTimestamp = arenaDataDbo.operationTimestamp,
			operationPosition = arenaDataDbo.operationPosition,
			before = arenaDataDbo.before?.let { mapper.readValue(it, D::class.java) },
			after = arenaDataDbo.after?.let { mapper.readValue(it, D::class.java) }
		)
	}

}

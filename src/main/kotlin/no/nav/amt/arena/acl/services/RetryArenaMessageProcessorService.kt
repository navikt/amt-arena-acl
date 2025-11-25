package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.consumer.ArenaDeltakerConsumer
import no.nav.amt.arena.acl.consumer.GjennomforingConsumer
import no.nav.amt.arena.acl.consumer.HistDeltakerConsumer
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.amt.arena.acl.exceptions.DependencyNotValidException
import no.nav.amt.arena.acl.exceptions.ExternalSourceSystemException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_HIST_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class RetryArenaMessageProcessorService(
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingConsumer: GjennomforingConsumer,
	private val arenaDeltakerConsumer: ArenaDeltakerConsumer,
	private val histDeltakerConsumer: HistDeltakerConsumer
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun processRetryMessages(batchSize: Int = DEFAULT_RETRY_MSG_BATCH_SIZE) =
		processMessagesWithStatus(IngestStatus.RETRY, batchSize)

	fun processFailedMessages(batchSize: Int = DEFAULT_FAILED_MSG_BATCH_SIZE) =
		processMessagesWithStatus(IngestStatus.FAILED, batchSize)

	private fun processMessagesWithStatus(status: IngestStatus, batchSize: Int) = listOf(
		ARENA_GJENNOMFORING_TABLE_NAME,
		ARENA_DELTAKER_TABLE_NAME,
		ARENA_HIST_DELTAKER_TABLE_NAME
	).forEach { tableName -> processMessages(tableName, status, batchSize) }

	private fun processMessages(tableName: String, status: IngestStatus, batchSize: Int) {
		val start = Instant.now()
		var totalHandled = 0

		var currentOperationPosition = 0.toOperationPosition()

		while (true) {
			val data = arenaDataRepository.getByIngestStatus(
				tableName = tableName,
				status = status,
				operationPosition = currentOperationPosition,
				limit = batchSize
			)

			if (data.isEmpty()) break

			data.forEach { arenaData -> processSingleArenaData(arenaData) }

			currentOperationPosition = data.last().operationPosition
			totalHandled += data.size
		}

		if (totalHandled > 0) {
			val duration = Duration.between(start, Instant.now())
			log.info("[$tableName]: Handled $totalHandled $status messages in ${duration.toSeconds()}.${duration.toMillisPart()} seconds.")
		}
	}

	private fun processSingleArenaData(arenaData: ArenaDataDbo) {
		runCatching {
			when (arenaData.arenaTableName) {
				ARENA_GJENNOMFORING_TABLE_NAME ->
					gjennomforingConsumer.handleArenaMessage(toArenaKafkaMessage(arenaData))

				ARENA_DELTAKER_TABLE_NAME ->
					arenaDeltakerConsumer.handleArenaMessage(toArenaKafkaMessage(arenaData))

				ARENA_HIST_DELTAKER_TABLE_NAME ->
					histDeltakerConsumer.handleArenaMessage(toArenaKafkaMessage(arenaData))
			}
		}.onFailure { throwable -> handleProcessingException(arenaData, throwable) }
	}

	private fun handleProcessingException(data: ArenaDataDbo, throwable: Throwable) {
		val prefix = "${data.id} (${data.arenaTableName})"
		val attempts = data.ingestAttempts + 1

		val newStatus = when (throwable) {
			is IgnoredException -> {
				log.info("$prefix: '${throwable.message}'")
				IngestStatus.IGNORED
			}

			is ExternalSourceSystemException -> {
				log.info("$prefix from external system: '${throwable.message}'")
				IngestStatus.EXTERNAL_SOURCE
			}

			is DependencyNotValidException -> {
				log.error("$prefix: '${throwable.message}'")
				IngestStatus.WAITING
			}

			else -> {
				if (data.ingestStatus == IngestStatus.RETRY && attempts >= MAX_INGEST_ATTEMPTS) {
					IngestStatus.FAILED
				} else {
					log.error("$prefix: ${throwable.message}", throwable)
					null
				}
			}
		}

		newStatus?.let { arenaDataRepository.updateIngestStatus(data.id, it) }
		arenaDataRepository.updateIngestAttempts(data.id, attempts, throwable.message)
	}

	companion object {
		private const val MAX_INGEST_ATTEMPTS = 10

		const val DEFAULT_RETRY_MSG_BATCH_SIZE = 5000
		const val DEFAULT_FAILED_MSG_BATCH_SIZE = 500

		// OPERATION_POS_LENGTH er hentet ut med følgende spørringer:
		// SELECT MIN(length(arena_data.operation_pos)) FROM arena_data
		// SELECT MAX(length(arena_data.operation_pos)) FROM arena_data
		private const val OPERATION_POS_LENGTH = 20
		private const val OPERATION_POS_PAD_CHAR = '0'

		fun Int.toOperationPosition() = this.toString().padStart(
			length = OPERATION_POS_LENGTH,
			padChar = OPERATION_POS_PAD_CHAR
		)

		private inline fun <reified D> toArenaKafkaMessage(arenaDataDbo: ArenaDataDbo): ArenaKafkaMessage<D> =
			ArenaKafkaMessage(
				arenaTableName = arenaDataDbo.arenaTableName,
				operationType = arenaDataDbo.operation,
				operationTimestamp = arenaDataDbo.operationTimestamp,
				operationPosition = arenaDataDbo.operationPosition,
				before = arenaDataDbo.before?.let { fromJsonString<D>(it) },
				after = arenaDataDbo.after?.let { fromJsonString<D>(it) }
			)
	}
}

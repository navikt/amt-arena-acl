package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.consumer.ArenaDeltakerConsumer
import no.nav.amt.arena.acl.consumer.GjennomforingConsumer
import no.nav.amt.arena.acl.consumer.HistDeltakerConsumer
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.extensions.toArenaKafkaMessage
import no.nav.amt.arena.acl.extensions.toOperationPosition
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_HIST_DELTAKER_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class RetryArenaMessageProcessorService(
	private val arenaDataRepository: ArenaDataRepository,
	private val gjennomforingConsumer: GjennomforingConsumer,
	private val arenaDeltakerConsumer: ArenaDeltakerConsumer,
	private val histDeltakerConsumer: HistDeltakerConsumer,
	private val retryMessageExceptionHandler: RetryMessageExceptionHandler,
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
					gjennomforingConsumer.handleArenaMessage(arenaData.toArenaKafkaMessage())

				ARENA_DELTAKER_TABLE_NAME ->
					arenaDeltakerConsumer.handleArenaMessage(arenaData.toArenaKafkaMessage())

				ARENA_HIST_DELTAKER_TABLE_NAME ->
					histDeltakerConsumer.handleArenaMessage(arenaData.toArenaKafkaMessage())
			}
		}.onFailure { throwable -> retryMessageExceptionHandler.handleProcessingException(arenaData, throwable) }
	}

	companion object {
		const val DEFAULT_RETRY_MSG_BATCH_SIZE = 5000
		const val DEFAULT_FAILED_MSG_BATCH_SIZE = 500
	}
}

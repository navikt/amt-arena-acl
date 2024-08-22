package no.nav.amt.arena.acl.services

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.db.toUpsertInput
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.exceptions.*
import no.nav.amt.arena.acl.processors.ArenaMessageProcessor
import no.nav.amt.arena.acl.processors.DeltakerProcessor
import no.nav.amt.arena.acl.processors.GjennomforingProcessor
import no.nav.amt.arena.acl.processors.HistDeltakerProcessor
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_HIST_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.DateUtils.parseArenaDateTime
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonNode
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.amt.arena.acl.utils.removeNullCharacters
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
open class ArenaMessageProcessorService(
	private val gjennomforingProcessor: GjennomforingProcessor,
	private val deltakerProcessor: DeltakerProcessor,
	private val histDeltakerProcessor: HistDeltakerProcessor,
	private val arenaDataRepository: ArenaDataRepository,
	private val meterRegistry: MeterRegistry
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun handleArenaGoldenGateRecord(record: ConsumerRecord<String, String>) {
		val recordValue = record.value().removeNullCharacters()
		val messageDto = fromJsonString<ArenaKafkaMessageDto>(recordValue)

		processArenaKafkaMessage(messageDto)
	}

	private fun processArenaKafkaMessage(messageDto: ArenaKafkaMessageDto) {
		val processorName = findProcessorName(messageDto.table)

		withTimer(processorName) {
			when (messageDto.table) {
				ARENA_GJENNOMFORING_TABLE_NAME -> process(messageDto, gjennomforingProcessor) { it.TILTAKGJENNOMFORING_ID.toString() }
				ARENA_DELTAKER_TABLE_NAME -> process(messageDto, deltakerProcessor) { it.TILTAKDELTAKER_ID.toString() }
				ARENA_HIST_DELTAKER_TABLE_NAME -> process(messageDto, histDeltakerProcessor) { it.TILTAKDELTAKER_ID.toString() }
				else -> throw IllegalArgumentException("Kan ikke håndtere melding fra ukjent arena tabell: ${messageDto.table}")
			}
		}
	}

	private inline fun <reified D> process(
		messageDto: ArenaKafkaMessageDto,
		processor: ArenaMessageProcessor<ArenaKafkaMessage<D>>,
		arenaIdExtractor: (msg: D) -> String
	) {
		val msg = toArenaKafkaMessage<D>(messageDto)
		val arenaId = arenaIdExtractor(msg.getData())
		val arenaTableName = msg.arenaTableName

		try {
			processor.handleArenaMessage(msg)
		} catch (e: Exception) {
			when (e) {
				is DependencyNotIngestedException -> {
					log.info("Dependency for $arenaId in table $arenaTableName is not ingested: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.RETRY, note = e.message))
				}
				is DependencyNotValidException -> {
					log.info("Dependency for $arenaId in table $arenaTableName is invalid: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.WAITING, note = e.message))
				}
				is ValidationException -> {
					log.info("$arenaId in table $arenaTableName is not valid: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.INVALID, note = e.message))
				}
				is IgnoredException -> {
					log.info("$arenaId in table $arenaTableName: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.IGNORED, note = e.message))
				}
				is OperationNotImplementedException -> {
					log.info("Operation not supported for $arenaId in table $arenaTableName: '${e.message}'")
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.FAILED, note = e.message))
				}
				else -> {
					log.error("$arenaId in table $arenaTableName: ${e.message}", e)
					arenaDataRepository.upsert(msg.toUpsertInput(arenaId, ingestStatus = IngestStatus.RETRY, note = e.message))
				}
			}
		}
	}

	private inline fun <reified D> toArenaKafkaMessage(messageDto: ArenaKafkaMessageDto): ArenaKafkaMessage<D> {
		return ArenaKafkaMessage(
			arenaTableName = messageDto.table,
			operationType = AmtOperation.fromArenaOperationString(messageDto.opType),
			operationTimestamp = parseArenaDateTime(messageDto.opTs),
			operationPosition = messageDto.pos,
			before = messageDto.before?.let { fromJsonNode<D>(it) },
			after =  messageDto.after?.let { fromJsonNode<D>(it) }
		)
	}

	private fun findProcessorName(arenaTableName: String): String {
		return when(arenaTableName) {
			ARENA_GJENNOMFORING_TABLE_NAME -> "gjennomforing"
			ARENA_DELTAKER_TABLE_NAME -> "deltaker"
			else -> "unknown"
		}
	}

	private fun withTimer(processorName: String, runnable: () -> Unit) {
		val timer = meterRegistry.timer(
			"amt.arena-acl.ingestStatus",
			listOf(Tag.of("processor", processorName))
		)

		timer.record<Unit> { runnable.invoke() }
	}
}

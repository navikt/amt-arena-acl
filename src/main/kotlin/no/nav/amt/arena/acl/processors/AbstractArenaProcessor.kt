package no.nav.amt.arena.acl.processors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.DigestUtils
import java.util.*

abstract class AbstractArenaProcessor<T>(
	protected val repository: ArenaDataRepository,
	private val clazz: Class<T>,
	private val kafkaProducer: KafkaProducerClient<String, String>,
	private val meterRegistry: MeterRegistry
) {

	@Value("\${app.env.amtTopic}")
	lateinit var topic: String

	private val logger = LoggerFactory.getLogger(javaClass)

	protected val objectMapper = jacksonObjectMapper()
		.registerModule(JavaTimeModule())
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

	companion object {
		private const val MAX_INGEST_ATTEMPTS = 10

		private val SUPPORTED_TILTAK = setOf(
			"INDOPPFAG",
		)
	}

	fun handle(data: ArenaData) {
		val timer = meterRegistry.timer(
			"amt.arena-acl.ingestStatus",
			listOf(Tag.of("processor", clazz.name))
		)

		timer.record {
			try {
				if (data.operation == AmtOperation.DELETED) {
					logger.error("Implementation for delete elements are not implemented. Cannot handle arena id ${data.arenaId} from table ${data.arenaTableName} at position ${data.operationPosition}.")
					repository.upsert(data.markAsFailed("Implementation of DELETE is not implemented."))
				} else {
					handleEntry(data)
				}
			} catch (e: Exception) {
				if (data.ingestAttempts >= MAX_INGEST_ATTEMPTS) {
					logger.error("[arena_data_id ${data.id}]: ${e.message}", e)
					repository.upsert(data.markAsFailed())
				} else {
					if (e !is DependencyNotIngestedException) {
						logger.error("[arena_data_id ${data.id}]: ${e.message}", e)
					}

					repository.upsert(data.retry())
				}
			}
		}
	}

	protected abstract fun handleEntry(data: ArenaData)

	protected fun getMainObject(data: ArenaData): T {
		return when (data.operation) {
			AmtOperation.CREATED -> jsonObject(data.after)
			AmtOperation.MODIFIED -> jsonObject(data.after)
			AmtOperation.DELETED -> jsonObject(data.before)
		}
			?: throw IllegalArgumentException("Expected ${data.arenaTableName} id ${data.arenaId} to have before or after correctly set.")
	}

	protected fun getDigest(data: Any): String {
		return DigestUtils.md5DigestAsHex(objectMapper.writeValueAsString(data).toByteArray())
	}

	protected fun send(groupKey: UUID, data: String) {
		kafkaProducer.sendSync(
			ProducerRecord(
				topic,
				groupKey.toString(),
				data
			)
		)
	}

	private fun jsonObject(node: JsonNode?): T? {
		if (node == null) {
			return null
		}

		return objectMapper.treeToValue(node, clazz)
	}


	protected fun <T> jsonObject(string: String?, clazz: Class<T>): T? {
		if (string == null) {
			return null
		}

		return objectMapper.readValue(string, clazz)
	}

	protected fun isSupportedTiltak(tiltakskode: String): Boolean {
		return SUPPORTED_TILTAK.contains(tiltakskode)
	}
}

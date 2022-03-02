package no.nav.amt.arena.acl.processors

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.exceptions.DependencyNotIngestedException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
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

	private val log = LoggerFactory.getLogger(javaClass)

	protected val objectMapper = ObjectMapperFactory.get()

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
				handleEntry(data)
			} catch (e: Exception) {
				if (data.ingestAttempts >= MAX_INGEST_ATTEMPTS) {
					log.error("${data.arenaId} in table ${data.arenaTableName}: ${e.message}", e)
					repository.upsert(data.markAsFailed(e.message))
				} else {
					if (e is DependencyNotIngestedException) {
						log.info("Dependency for ${data.arenaId} in table ${data.arenaTableName} is not ingested: '${e.message}'")
						repository.upsert(data.retry(e.message))
					} else if (e is ValidationException) {
						log.info("${data.arenaId} in table ${data.arenaTableName} is not valid: '${e.validationMessage}'.")
						repository.upsert(data.markAsInvalid(e.validationMessage))
					} else if (e is IgnoredException) {
						log.info("${data.arenaId} in table ${data.arenaTableName}: '${e.message}'")
						repository.upsert(data.markAsIgnored(e.message))
					} else {
						log.error("${data.arenaId} in table ${data.arenaTableName}: ${e.message}", e)
						repository.upsert(data.retry())
					}
				}
			}
		}
	}

	protected abstract fun handleEntry(data: ArenaData)


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

	protected fun isSupportedTiltak(tiltakskode: String): Boolean {
		return SUPPORTED_TILTAK.contains(tiltakskode)
	}
}

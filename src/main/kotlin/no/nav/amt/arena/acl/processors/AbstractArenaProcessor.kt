package no.nav.amt.arena.acl.processors

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import org.slf4j.LoggerFactory
import org.springframework.util.DigestUtils

abstract class AbstractArenaProcessor<T>(
	private val clazz: Class<T>,
	private val repository: ArenaDataRepository,
	private val idTranslationRepository: ArenaDataIdTranslationRepository,
) {

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

	abstract fun handle(data: ArenaData)

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


	protected fun jsonObject(string: String?): T? {
		if (string == null) {
			return null
		}

		return objectMapper.readValue(string, clazz)
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

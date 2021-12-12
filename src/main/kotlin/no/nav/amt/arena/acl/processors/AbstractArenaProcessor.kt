package no.nav.amt.arena.acl.processors

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.amt.arena.acl.domain.ArenaData

abstract class AbstractArenaProcessor {

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

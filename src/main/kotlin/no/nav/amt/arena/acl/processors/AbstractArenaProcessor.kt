package no.nav.amt.arena.acl.processors

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.amt.arena.acl.domain.ArenaData
import org.slf4j.LoggerFactory

abstract class AbstractArenaProcessor {

	private val logger = LoggerFactory.getLogger(javaClass)

	protected val objectMapper = jacksonObjectMapper()

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

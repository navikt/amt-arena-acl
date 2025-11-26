package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.exceptions.DependencyNotValidException
import no.nav.amt.arena.acl.exceptions.ExternalSourceSystemException
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RetryMessageExceptionHandler(
	private val arenaDataRepository: ArenaDataRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun handleProcessingException(arenaData: ArenaDataDbo, throwable: Throwable) {
		val prefix = "${arenaData.id} (${arenaData.arenaTableName})"
		val attempts = arenaData.ingestAttempts + 1

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
				if (arenaData.ingestStatus == IngestStatus.RETRY && attempts >= MAX_INGEST_ATTEMPTS) {
					IngestStatus.FAILED
				} else {
					log.error("$prefix: ${throwable.message}", throwable)
					null
				}
			}
		}

		newStatus?.let { arenaDataRepository.updateIngestStatus(arenaData.id, it) }
		arenaDataRepository.updateIngestAttempts(arenaData.id, attempts, throwable.message)
	}

	companion object {
		private const val MAX_INGEST_ATTEMPTS = 10
	}
}

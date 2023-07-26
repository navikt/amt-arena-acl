package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
open class ArenaDataIdTranslationService(
	private val arenaDataIdTranslationRepository: ArenaDataIdTranslationRepository
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun hentArenaId(id: UUID): String? {
		return arenaDataIdTranslationRepository.get(id)?.arenaId
	}

	fun hentEllerOpprettNyDeltakerId(deltakerArenaId: String): UUID {
		val deltakerId = arenaDataIdTranslationRepository.get(ARENA_DELTAKER_TABLE_NAME, deltakerArenaId)?.amtId

		if (deltakerId == null) {
			val nyDeltakerIdId = UUID.randomUUID()
			arenaDataIdTranslationRepository.insert(
				ArenaDataIdTranslationDbo(
					amtId = nyDeltakerIdId,
					arenaTableName = ARENA_DELTAKER_TABLE_NAME,
					arenaId = deltakerArenaId
				)
			)

			log.info("Opprettet ny id for deltaker, id=$nyDeltakerIdId arenaId=$deltakerArenaId")
			return nyDeltakerIdId
		}

		return deltakerId
	}

}

package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.db.ArenaDataHistIdTranslationDbo
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.repositories.ArenaDataHistIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
open class ArenaDataIdTranslationService(
	private val arenaDataIdTranslationRepository: ArenaDataIdTranslationRepository,
	private val arenaDataHistIdTranslationRepository: ArenaDataHistIdTranslationRepository
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun hentArenaIdEllerHistId(id: UUID): String? {
		return arenaDataIdTranslationRepository.get(id)?.arenaId
			?: arenaDataHistIdTranslationRepository.get(id)?.arenaHistId
	}

	fun hentArenaHistId(id: UUID): String? {
		return arenaDataHistIdTranslationRepository.get(id)?.arenaHistId
	}

	fun hentArenaId(id: UUID): String? {
		return arenaDataIdTranslationRepository.get(id)?.arenaId
	}

	fun hentAmtId(arenaId: String, table: String = ARENA_DELTAKER_TABLE_NAME): UUID? {
		return arenaDataIdTranslationRepository.get(table, arenaId)?.amtId
	}

	fun hentEllerOpprettNyDeltakerId(deltakerArenaId: String, table: String = ARENA_DELTAKER_TABLE_NAME): UUID {
		val deltakerId = arenaDataIdTranslationRepository.get(table, deltakerArenaId)?.amtId

		if (deltakerId == null) {
			val nyDeltakerId = UUID.randomUUID()
			arenaDataIdTranslationRepository.insert(
				ArenaDataIdTranslationDbo(
					amtId = nyDeltakerId,
					arenaTableName = ARENA_DELTAKER_TABLE_NAME,
					arenaId = deltakerArenaId
				)
			)

			log.info("Opprettet ny id for deltaker, id=$nyDeltakerId arenaId=$deltakerArenaId")
			return nyDeltakerId
		}

		return deltakerId
	}

	fun opprettIdTranslation(arenaId: String, amtId: UUID, table: String = ARENA_DELTAKER_TABLE_NAME) {
		arenaDataIdTranslationRepository.insert(
			ArenaDataIdTranslationDbo(
			amtId = amtId,
			arenaTableName = table,
			arenaId = arenaId
		))
		log.info("Opprettet ny id for deltaker, id=$amtId arenaId=$arenaId")

	}

	fun lagreHistDeltakerId(
		amtDeltakerId: UUID,
		histDeltakerArenaId: String
	) {
		arenaDataHistIdTranslationRepository.insert(
			ArenaDataHistIdTranslationDbo(
				amtId = amtDeltakerId,
				arenaHistId = histDeltakerArenaId,
				arenaId = arenaDataIdTranslationRepository.get(amtDeltakerId)?.arenaId
			)
		)
	}
}

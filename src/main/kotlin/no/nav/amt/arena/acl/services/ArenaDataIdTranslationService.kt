package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
open class ArenaDataIdTranslationService(
	private val arenaDataIdTranslationRepository: ArenaDataIdTranslationRepository
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun upsertGjennomforingIdTranslation(gjennomforingArenaId: String, gjennomforingAmtId: UUID) {
		upsertTranslation(
			arenaId = gjennomforingArenaId,
			amtId = gjennomforingAmtId,
			arenaTableName = ARENA_GJENNOMFORING_TABLE_NAME
		)
	}

	fun upsertDeltakerIdTranslation(deltakerArenaId: String, deltakerAmtId: UUID) {
		upsertTranslation(
			arenaId = deltakerArenaId,
			amtId = deltakerAmtId,
			arenaTableName = ARENA_DELTAKER_TABLE_NAME,
		)
	}


	fun findGjennomforingIdTranslation(gjennomforingArenaId: String): ArenaDataIdTranslationDbo? {
		return arenaDataIdTranslationRepository.get(ARENA_GJENNOMFORING_TABLE_NAME, gjennomforingArenaId)
	}

	fun hentEllerOpprettNyGjennomforingId(gjennomforingArenaId: String): UUID {
		val gjennomforingId =
			arenaDataIdTranslationRepository.get(ARENA_GJENNOMFORING_TABLE_NAME, gjennomforingArenaId)?.amtId

		if (gjennomforingId == null) {
			val nyGjennomforingId = UUID.randomUUID()
			arenaDataIdTranslationRepository.insert(
				ArenaDataIdTranslationDbo(
					amtId = nyGjennomforingId,
					arenaTableName = ARENA_GJENNOMFORING_TABLE_NAME,
					arenaId = gjennomforingArenaId
				)
			)
			log.info("Opprettet ny id for gjennomføring, id=$nyGjennomforingId arenaId=$gjennomforingArenaId")
			return nyGjennomforingId
		}

		return gjennomforingId
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

	private fun upsertTranslation(arenaId: String, arenaTableName: String, amtId: UUID) {
		val translation = ArenaDataIdTranslationDbo(
			amtId = amtId,
			arenaTableName = arenaTableName,
			arenaId = arenaId,
		)

		arenaDataIdTranslationRepository.insert(translation)
	}

}

package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
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

	fun upsertGjennomforingIdTranslation(gjennomforingArenaId: String, gjennomforingAmtId: UUID, ignored: Boolean) {
		upsertTranslation(
			arenaId = gjennomforingArenaId,
			amtId = gjennomforingAmtId,
			arenaTableName = ARENA_GJENNOMFORING_TABLE_NAME,
			ignored = ignored
		)
	}

	fun upsertDeltakerIdTranslation(deltakerArenaId: String, deltakerAmtId: UUID, ignored: Boolean) {
		upsertTranslation(
			arenaId = deltakerArenaId,
			amtId = deltakerAmtId,
			arenaTableName = ARENA_DELTAKER_TABLE_NAME,
			ignored = ignored
		)
	}

	fun findGjennomforingIdTranslation(gjennomforingArenaId: String): ArenaDataIdTranslation?  {
		return arenaDataIdTranslationRepository.get(ARENA_GJENNOMFORING_TABLE_NAME, gjennomforingArenaId)
	}

	fun hentEllerOpprettNyGjennomforingId(gjennomforingArenaId: String): UUID {
		val gjennomforingId = arenaDataIdTranslationRepository.getAmtId(ARENA_GJENNOMFORING_TABLE_NAME, gjennomforingArenaId)

		if (gjennomforingId == null) {
			val nyGjennomforingId = UUID.randomUUID()
			log.info("Opprettet ny id for gjennomføring, id=$nyGjennomforingId arenaId=$gjennomforingArenaId")
			return nyGjennomforingId
		}

		return gjennomforingId
	}

	fun hentEllerOpprettNyDeltakerId(deltakerArenaId: String): UUID {
		val deltakerId = arenaDataIdTranslationRepository.getAmtId(ARENA_DELTAKER_TABLE_NAME, deltakerArenaId)

		if (deltakerId == null) {
			val nyDeltakerIdId = UUID.randomUUID()
			log.info("Opprettet ny id for deltaker, id=$nyDeltakerIdId arenaId=$deltakerArenaId")
			return nyDeltakerIdId
		}

		return deltakerId
	}

	private fun upsertTranslation(arenaId: String, arenaTableName: String, amtId: UUID, ignored: Boolean) {
		val translation = ArenaDataIdTranslation(
			amtId = amtId,
			arenaTableName = arenaTableName,
			arenaId = arenaId,
			ignored = ignored,
		)

		arenaDataIdTranslationRepository.insert(translation)
	}

}

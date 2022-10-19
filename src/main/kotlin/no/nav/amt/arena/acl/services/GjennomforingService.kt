package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.repositories.ArenaGjennomforingDbo
import no.nav.amt.arena.acl.repositories.ArenaGjennomforingRepository
import no.nav.amt.arena.acl.repositories.IgnoredArenaDataRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class GjennomforingService(
	private val arenaGjennomforingRepository: ArenaGjennomforingRepository,
	private val ignoredRepository: IgnoredArenaDataRepository,
	private val translationService: ArenaDataIdTranslationService
) {

	private val SUPPORTED_TILTAK = setOf(
		"INDOPPFAG",
		"ARBFORB",
		"AVKLARAG",
		"VASV",
		"ARBRRHDAG",
		"DIGIOPPARB"
	)

	fun ignore(id: UUID) {
		ignoredRepository.ignore(id)
	}

	fun isIgnored(id: UUID): Boolean {
		return ignoredRepository.isIgnored(id)
	}

	fun isSupportedTiltak(kode: String): Boolean {
		return SUPPORTED_TILTAK.contains(kode)
	}

	fun upsert(dbo: ArenaGjennomforingDbo) {
		arenaGjennomforingRepository.upsert(dbo)
	}

	fun getGjennomforing(id: UUID): ArenaGjennomforingDbo? {
		return arenaGjennomforingRepository.get(id)
	}
}

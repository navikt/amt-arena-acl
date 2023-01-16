package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.repositories.*
import org.springframework.stereotype.Service
import java.util.*

val SUPPORTED_TILTAK = setOf(
	"INDOPPFAG",
	"ARBFORB",
	"AVKLARAG",
	"VASV",
	"ARBRRHDAG",
	"DIGIOPPARB"
)

@Service
class GjennomforingService(
	private val arenaGjennomforingRepository: ArenaGjennomforingRepository,
	private val ignoredRepository: IgnoredArenaDataRepository,
	private val gjennomforingRepository: GjennomforingRepository
) {

	fun ignore(id: UUID) {
		ignoredRepository.ignore(id)
	}

	fun upsert(arenaId: String, tiltakKode: String, isValid: Boolean) {
		gjennomforingRepository.upsert(arenaId, tiltakKode, isValid)
	}

	fun get(arenaId: String): Gjennomforing? {
		return gjennomforingRepository.get(arenaId)?.toModel()
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
	data class Gjennomforing (
		val arenaId: String,
		val tiltakKode: String,
		val isValid: Boolean,
	) {
		val isSupported = SUPPORTED_TILTAK.contains(tiltakKode)
	}
}

fun GjennomforingDbo.toModel() = GjennomforingService.Gjennomforing(
	arenaId = arenaId,
	tiltakKode = tiltakKode,
	isValid = isValid
)

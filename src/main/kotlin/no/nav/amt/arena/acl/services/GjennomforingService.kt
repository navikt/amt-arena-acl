package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.repositories.*
import org.springframework.stereotype.Service

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
	private val gjennomforingRepository: GjennomforingRepository
) {
	fun upsert(arenaId: String, tiltakKode: String, isValid: Boolean) {
		gjennomforingRepository.upsert(arenaId, tiltakKode, isValid)
	}

	fun get(arenaId: String): Gjennomforing? {
		return gjennomforingRepository.get(arenaId)?.toModel()
	}


	fun isSupportedTiltak(kode: String): Boolean {
		return SUPPORTED_TILTAK.contains(kode)
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

package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.Gjennomforing
import no.nav.amt.arena.acl.repositories.GjennomforingDbo
import no.nav.amt.arena.acl.repositories.GjennomforingRepository
import org.springframework.stereotype.Service
import java.util.UUID

val SUPPORTED_TILTAK =
	setOf(
		"INDOPPFAG",
		"ARBFORB",
		"AVKLARAG",
		"VASV",
		"ARBRRHDAG",
		"DIGIOPPARB",
		"JOBBK",
		"GRUPPEAMO",
		"GRUFAGYRKE",
	)

@Service
class GjennomforingService(
	private val gjennomforingRepository: GjennomforingRepository,
) {
	fun upsert(
		arenaId: String,
		tiltakKode: String,
		isValid: Boolean,
	) {
		gjennomforingRepository.upsert(arenaId, tiltakKode, isValid)
	}

	fun get(arenaId: String): Gjennomforing? = gjennomforingRepository.get(arenaId)?.toModel()

	fun setGjennomforingId(
		arenaId: String,
		gjennomforingId: UUID,
	) = gjennomforingRepository.updateGjennomforingId(arenaId, gjennomforingId)

	fun isSupportedTiltak(kode: String): Boolean = SUPPORTED_TILTAK.contains(kode)
}

fun GjennomforingDbo.toModel() =
	Gjennomforing(
		arenaId = arenaId,
		tiltakKode = tiltakKode,
		isValid = isValid,
		id = id,
	)

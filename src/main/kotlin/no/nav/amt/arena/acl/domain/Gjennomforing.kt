package no.nav.amt.arena.acl.domain

import java.util.UUID

data class Gjennomforing (
	val arenaId: String,
	val tiltakKode: String,
	val isValid: Boolean,
	val id: UUID?
) {
	val isSupported = SUPPORTED_TILTAK.contains(tiltakKode)

	companion object {
		val SUPPORTED_TILTAK = setOf(
			"INDOPPFAG",
			"ARBFORB",
			"AVKLARAG",
			"VASV",
			"ARBRRHDAG",
			"DIGIOPPARB",
			"JOBBK",
			"GRUPPEAMO",
			"GRUFAGYRKE"
		)

		fun isSupportedTiltak(kode: String): Boolean = SUPPORTED_TILTAK.contains(kode)
	}
}

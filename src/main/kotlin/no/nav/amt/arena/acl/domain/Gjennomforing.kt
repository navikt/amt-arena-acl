package no.nav.amt.arena.acl.domain

import no.nav.amt.arena.acl.services.SUPPORTED_TILTAK
import java.util.*

data class Gjennomforing (
	val arenaId: String,
	val tiltakKode: String,
	val isValid: Boolean,
	val id: UUID?
) {
	val isSupported = SUPPORTED_TILTAK.contains(tiltakKode)
}

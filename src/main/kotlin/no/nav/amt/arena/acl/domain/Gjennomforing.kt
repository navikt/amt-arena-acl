package no.nav.amt.arena.acl.domain

import java.util.UUID

data class Gjennomforing (
	val arenaId: String,
	val tiltakKode: String,
	val isValid: Boolean,
	val id: UUID?
)

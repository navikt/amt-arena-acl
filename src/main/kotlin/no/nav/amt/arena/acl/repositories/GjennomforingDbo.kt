package no.nav.amt.arena.acl.repositories

import java.time.LocalDate

data class GjennomforingDbo(
	val arenaId: String,
	val tiltakKode: String,
	val isValid: Boolean,
	val createdAt: LocalDate
)

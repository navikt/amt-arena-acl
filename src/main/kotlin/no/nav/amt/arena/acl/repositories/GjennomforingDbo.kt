package no.nav.amt.arena.acl.repositories

import java.time.LocalDate
import java.util.*

data class GjennomforingDbo(
	val arenaId: String,
	val tiltakKode: String,
	val isValid: Boolean,
	val id: UUID?,
	val createdAt: LocalDate
)

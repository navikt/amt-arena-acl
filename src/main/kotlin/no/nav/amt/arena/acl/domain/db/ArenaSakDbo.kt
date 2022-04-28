package no.nav.amt.arena.acl.domain.db

import java.time.LocalDateTime

data class ArenaSakDbo(
	val id: Int,
	val arenaSakId: Long,
	val aar: Int,
	val lopenr: Int,
	val ansvarligEnhetId: String,
	val createdAt: LocalDateTime
)

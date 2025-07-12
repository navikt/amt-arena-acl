package no.nav.amt.arena.acl.domain.db

import java.util.UUID

data class ArenaDataHistIdTranslationDbo(
	val amtId: UUID,
	val arenaHistId: String,
	val arenaId: String?,
)

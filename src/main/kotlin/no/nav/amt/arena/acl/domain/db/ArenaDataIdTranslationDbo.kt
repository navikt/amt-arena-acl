package no.nav.amt.arena.acl.domain.db

import java.util.UUID

data class ArenaDataIdTranslationDbo(
	val amtId: UUID,
	val arenaTableName: String,
	val arenaId: String,
)

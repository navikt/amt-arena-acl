package no.nav.amt.arena.acl.domain.db

import java.util.*

data class ArenaDataIdTranslationDbo(
	val amtId: UUID,
	val arenaTableName: String,
	val arenaId: String,
	val ignored: Boolean,
)

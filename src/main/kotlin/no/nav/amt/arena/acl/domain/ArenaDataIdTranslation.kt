package no.nav.amt.arena.acl.domain

import java.util.*

data class ArenaDataIdTranslation(
	val amtId: UUID,
	val arenaTableName: String,
	val arenaId: String,
	val ignored: Boolean = false
)

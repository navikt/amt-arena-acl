package no.nav.amt.arena.acl.domain

import java.util.*

enum class Creation {
	CREATED,
	EXISTED
}

data class ArenaDataIdTranslation(
	val amtId: UUID,
	val arenaTableName: String,
	val arenaId: String,
	val ignored: Boolean,
	val currentHash: String
)

package no.nav.amt.arena.acl.application.models

import no.nav.amt.arena.acl.application.models.arena.ArenaOpType

enum class OperationType {
    INSERT,
    UPDATE,
    DELETE;

    companion object {
        fun fromArena(string: ArenaOpType): OperationType {
            return when (string) {
                ArenaOpType.I -> INSERT
                ArenaOpType.U -> UPDATE
                ArenaOpType.D -> DELETE
            }
        }
    }

}

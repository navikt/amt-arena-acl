package no.nav.amt.arena.acl.extensions

// OPERATION_POS_LENGTH er hentet ut med følgende spørringer (begge returnerte 20):
// SELECT MIN(length(arena_data.operation_pos)) FROM arena_data
// SELECT MAX(length(arena_data.operation_pos)) FROM arena_data
private const val OPERATION_POS_LENGTH = 20
private const val OPERATION_POS_PAD_CHAR = '0'

fun Int.toOperationPosition() = this.toString().padStart(
	length = OPERATION_POS_LENGTH,
	padChar = OPERATION_POS_PAD_CHAR
)

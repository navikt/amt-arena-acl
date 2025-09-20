package no.nav.amt.arena.acl.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateUtils {
	private val arenaDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

	fun parseArenaDateTime(arenaDateTime: String): LocalDateTime =
		LocalDateTime.parse(arenaDateTime, arenaDateTimeFormatter)

	val localDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
}

package no.nav.amt.arena.acl.integration.utils

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs

object DateUtils {

	fun isEqual(date1: LocalDateTime, date2: LocalDateTime, within: Duration = Duration.ofSeconds(1)): Boolean {
		val date1Millis = date1.toInstant(ZoneOffset.UTC).toEpochMilli()
		val date2Millis = date2.toInstant(ZoneOffset.UTC).toEpochMilli()

		return abs(date1Millis - date2Millis) <= within.toMillis()
	}

}

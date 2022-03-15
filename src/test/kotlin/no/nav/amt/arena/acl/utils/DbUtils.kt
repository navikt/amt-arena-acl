package no.nav.amt.arena.acl.utils

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object DbUtils {

	/**
	 * A helping function as SQL Timestamp and LocalDateTime does not have the same precision
	 */
	fun LocalDateTime.isEqualTo(other: LocalDateTime?): Boolean {
		if (other == null) {
			return false
		}

		return this.truncatedTo(ChronoUnit.SECONDS) == other.truncatedTo(ChronoUnit.SECONDS)
	}

}

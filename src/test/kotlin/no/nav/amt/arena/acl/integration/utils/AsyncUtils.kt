package no.nav.amt.arena.acl.integration.utils

import org.slf4j.LoggerFactory
import java.lang.AssertionError
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

object AsyncUtils {

	fun eventually(
		until: Duration = Duration.ofSeconds(10),
		interval: Duration = Duration.ofMillis(100),
		func: () -> Unit
	) {
		val untilTime = LocalDateTime.now().plusNanos(until.toNanos())

		var throwable: Throwable = IllegalStateException()

		while (LocalDateTime.now().isBefore(untilTime)) {
			try {
				func()
				return
			} catch (t: Throwable) {
				throwable = t
				Thread.sleep(interval.toMillis())
			}
		}

		throw AssertionError(throwable)
	}
}

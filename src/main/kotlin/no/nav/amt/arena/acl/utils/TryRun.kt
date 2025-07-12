package no.nav.amt.arena.acl.utils

inline fun <T, R> T.tryRun(fn: (t: T) -> R): Result<R> =
	try {
		Result.success(fn(this))
	} catch (t: Throwable) {
		Result.failure(t)
	}

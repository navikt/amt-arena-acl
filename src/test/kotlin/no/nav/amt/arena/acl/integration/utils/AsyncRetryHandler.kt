package no.nav.amt.arena.acl.integration.utils

fun <T> asyncRetryHandler(
	executor: () -> T?,
	maxAttempts: Int = 20,
	sleepTime: Long = 250
): T {
	return nullableAsyncRetryHandler(executor, maxAttempts, sleepTime)
		?: throw IllegalStateException("Did not find data in $maxAttempts attempts.")
}

fun <T> nullableAsyncRetryHandler(
	executor: () -> T?,
	maxAttempts: Int = 20,
	sleepTime: Long = 250
): T? {
	var attempts = 0
	while (attempts < maxAttempts) {
		val data = executor()

		if (data != null) {
			return data
		}

		Thread.sleep(sleepTime)
		attempts++
	}
	return null
}

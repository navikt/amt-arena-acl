package no.nav.amt.arena.acl.exceptions

class DependencyNotIngestedException(
	m: String,
	exception: Exception? = null
) : Exception(m, exception)

package no.nav.amt.arena.acl.domain.db

enum class IngestStatus {
	NEW,
	HANDLED,
	RETRY,
	FAILED,
	IGNORED,
	INVALID,
	WAITING,
	EXTERNAL_SOURCE
}

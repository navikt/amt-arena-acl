package no.nav.amt.arena.acl.application.models

enum class IngestStatus {
    NEW,
    INGESTED,
    RETRY,
    FAILED,
	IGNORED
}

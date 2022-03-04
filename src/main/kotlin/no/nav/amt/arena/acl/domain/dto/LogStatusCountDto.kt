package no.nav.amt.arena.acl.domain.dto

import no.nav.amt.arena.acl.domain.db.IngestStatus

data class LogStatusCountDto(
	val status: IngestStatus,
	val count: Int
)

package no.nav.amt.arena.acl.domain.kafka.amt

import java.util.*

data class AmtTiltak(
	val id: UUID,
	val kode: String,
	val navn: String
)

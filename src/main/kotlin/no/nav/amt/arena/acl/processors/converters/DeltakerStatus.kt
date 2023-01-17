package no.nav.amt.arena.acl.processors.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import java.time.LocalDateTime

data class DeltakerStatus(
	val navn: AmtDeltaker.Status,
	val endretDato: LocalDateTime?
)

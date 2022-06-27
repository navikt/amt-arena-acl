package no.nav.amt.arena.acl.processors.converters

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import java.time.LocalDate

internal data class Status(
    val navn: AmtDeltaker.Status,
    val endretDato: LocalDate?
)

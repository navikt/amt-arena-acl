package no.nav.amt.arena.acl.domain.amt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class AmtGjennomforing(
	val id: UUID,
	val tiltakId: UUID,
	val virksomhetsnummer: String,
	val navn: String,
	val oppstartDato: LocalDate?,
	val sluttDato: LocalDate?,
	val registrert: LocalDateTime,
	val fremmote: LocalDateTime?
)

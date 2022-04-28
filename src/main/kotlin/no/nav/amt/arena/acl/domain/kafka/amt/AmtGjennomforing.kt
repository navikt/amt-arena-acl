package no.nav.amt.arena.acl.domain.kafka.amt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class AmtGjennomforing(
	val id: UUID,
	val tiltak: AmtTiltak,
	val virksomhetsnummer: String,
	val navn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val registrertDato: LocalDateTime,
	val fremmoteDato: LocalDateTime?,
	val status: Status,

	// Hentet fra Sak
	val ansvarligNavEnhetId: String?,
	val sakAar: Int?,
	val sakLopenr: Int?,
) {
	enum class Status {
		IKKE_STARTET,
		GJENNOMFORES,
		AVSLUTTET
	}
}

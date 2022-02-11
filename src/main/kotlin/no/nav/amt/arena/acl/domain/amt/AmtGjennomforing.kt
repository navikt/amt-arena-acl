package no.nav.amt.arena.acl.domain.amt

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class AmtGjennomforing(
	@JsonProperty("id") val id: UUID,
	@JsonProperty("tiltak") val tiltak: AmtTiltak,
	@JsonProperty("virksomhetsnummer") val virksomhetsnummer: String,
	@JsonProperty("navn") val navn: String,
	@JsonProperty("startDato") val startDato: LocalDate?,
	@JsonProperty("sluttDato") val sluttDato: LocalDate?,
	@JsonProperty("registrertDato") val registrertDato: LocalDateTime,
	@JsonProperty("fremmoteDato") val fremmoteDato: LocalDateTime?,
	@JsonProperty("status") val status: Status
) {
	enum class Status {
		IKKE_STARTET,
		GJENNOMFORES,
		AVSLUTTET
	}
}

package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import java.time.LocalDate
import java.util.UUID

data class Gjennomforing(
	val id: UUID,
	val tiltakstype: Tiltakstype,
	val navn: String? = null,
	val startDato: LocalDate? = null,
	val sluttDato: LocalDate? = null,
	val status: Status? = null,
	val virksomhetsnummer: String,
	val oppstart: Oppstartstype? = null,
) {
	enum class Oppstartstype {
		LOPENDE,
		FELLES,
	}

	data class Tiltakstype(
		val id: UUID? = null,
		val navn: String? = null,
		val arenaKode: String,
	)

	enum class Status {
		GJENNOMFORES,
		AVBRUTT,
		AVLYST,
		AVSLUTTET,
	}

	fun erAvsluttet(): Boolean = status in listOf(Status.AVSLUTTET, Status.AVBRUTT, Status.AVLYST)

	fun erKurs(): Boolean = oppstart == Oppstartstype.FELLES

	fun erEnkelplass(): Boolean = tiltakstype.arenaKode in setOf("ENKELAMO", "ENKFAGYRKE", "HOYEREUTD")
}

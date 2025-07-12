package no.nav.amt.arena.acl.clients.mulighetsrommetapi

import java.time.LocalDate
import java.util.UUID

interface MulighetsrommetApiClient {
	fun hentGjennomforingId(arenaId: String): UUID?

	fun hentGjennomforing(id: UUID): Gjennomforing
}

data class Gjennomforing(
	val id: UUID,
	val tiltakstype: Tiltakstype,
	val navn: String,
	val startDato: LocalDate,
	val sluttDato: LocalDate? = null,
	val status: Status,
	val virksomhetsnummer: String,
	val oppstart: Oppstartstype,
) {
	enum class Oppstartstype {
		LOPENDE,
		FELLES,
	}

	data class Tiltakstype(
		val id: UUID,
		val navn: String,
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
}

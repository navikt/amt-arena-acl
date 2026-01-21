package no.nav.amt.arena.acl.clients.amttiltak

import java.time.LocalDate
import java.util.UUID

data class DeltakerDto(
	val id: UUID,
	val gjennomforing: GjennomforingDto,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: DeltakerStatus,
)

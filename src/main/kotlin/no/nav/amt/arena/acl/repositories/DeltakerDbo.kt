package no.nav.amt.arena.acl.repositories

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerDbo(
	val arenaId: Long?,
	val personId: Long?,
	val gjennomforingId: Long?,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val regDato: LocalDateTime,
	val modDato: LocalDateTime,
	val status: String?,
	val datoStatusEndring: LocalDateTime?,
	val arenaSourceTable: String,
	val eksternId: UUID?,
	val createdAt: LocalDateTime,
	val modifiedAt: LocalDateTime,
)

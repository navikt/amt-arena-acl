package no.nav.amt.arena.acl.domain.kafka.arena

import java.time.LocalDate
import java.time.LocalDateTime


data class TiltakGjennomforing(
	val tiltakgjennomforingId: String,
	val sakId: Long,
	val tiltakskode: String,
	val arbgivIdArrangor: String,
	val lokaltNavn: String,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val datoFremmote: LocalDateTime?,
	val tiltakstatusKode: String,
	val regDato: LocalDateTime
)

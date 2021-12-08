package no.nav.amt.arena.acl.domain.amt

import java.time.LocalDate
import java.util.*

enum class AmtDeltakerStatus {
	NY_BRUKER, GJENNOMFORES, AVBRUTT, FULLFORT
}

data class AmtDeltaker(
	val id: UUID,
	val gjennomforingId: UUID,
	val personIdent: String,
	val oppstartDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: AmtDeltakerStatus,
	val dagerPerUke: Int?,
	val prosentDeltid: Float?
) : AmtPayload

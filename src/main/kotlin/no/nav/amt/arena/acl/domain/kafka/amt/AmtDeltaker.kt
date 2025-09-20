package no.nav.amt.arena.acl.domain.kafka.amt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AmtDeltaker(
	val id: UUID,
	val gjennomforingId: UUID,
	val personIdent: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: Status,
	val statusAarsak: StatusAarsak?,
	val dagerPerUke: Float?,
	val prosentDeltid: Float?,
	val registrertDato: LocalDateTime,
	val statusEndretDato: LocalDateTime?,
	val innsokBegrunnelse: String?
) {
	enum class Status {
		VENTER_PA_OPPSTART, DELTAR, HAR_SLUTTET, IKKE_AKTUELL, FEILREGISTRERT, PABEGYNT_REGISTRERING,
		SOKT_INN, VURDERES, VENTELISTE, AVBRUTT, FULLFORT; // kurs statuser

		fun erAvsluttende() : Boolean = this in setOf(
			IKKE_AKTUELL,
			HAR_SLUTTET,
			FEILREGISTRERT,
			AVBRUTT,
			FULLFORT
		)
	}

	enum class StatusAarsak {
		SYK,
		FATT_JOBB,
		TRENGER_ANNEN_STOTTE,
		FIKK_IKKE_PLASS,
		AVLYST_KONTRAKT,
		IKKE_MOTT,
		ANNET
	}

	fun toFeilregistrertDeltaker() =
		this.copy(
			status = Status.FEILREGISTRERT,
			statusAarsak = null,
			dagerPerUke = null,
			prosentDeltid = null,
			innsokBegrunnelse = null
		)
}

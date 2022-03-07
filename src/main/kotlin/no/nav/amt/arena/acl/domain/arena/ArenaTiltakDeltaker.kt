package no.nav.amt.arena.acl.domain.arena

import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.utils.asValidatedLocalDate
import no.nav.amt.arena.acl.utils.asValidatedLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime

// @SONAR_START@
data class ArenaTiltakDeltaker(
	val TILTAKDELTAKER_ID: Long,
	val PERSON_ID: Long?,
	val TILTAKGJENNOMFORING_ID: Long,
	val DELTAKERSTATUSKODE: String,
	val DELTAKERTYPEKODE: String?,
	val AARSAKVERDIKODE_STATUS: String?,
	val OPPMOTETYPEKODE: String?,
	val PRIORITET: Int?,
	val BEGRUNNELSE_INNSOKT: String?,
	val BEGRUNNELSE_PRIORITERING: String?,
	val REG_DATO: String?,
	val REG_USER: String?,
	val MOD_DATO: String?,
	val MOD_USER: String?,
	val DATO_SVARFRIST: String?,
	val DATO_FRA: String?,
	val DATO_TIL: String?,
	val BEGRUNNELSE_STATUS: String?,
	val PROSENT_DELTID: Float?,
	val BRUKERID_STATUSENDRING: String,
	val DATO_STATUSENDRING: String?,
	val AKTIVITET_ID: Long,
	val BRUKERID_ENDRING_PRIORITERING: String?,
	val DATO_ENDRING_PRIORITERING: String?,
	val DOKUMENTKODE_SISTE_BREV: String?,
	val STATUS_INNSOK_PAKKE: String?,
	val STATUS_OPPTAK_PAKKE: String?,
	val OPPLYSNINGER_INNSOK: String?,
	val PARTISJON: Int?,
	val BEGRUNNELSE_BESTILLING: String?,
	val ANTALL_DAGER_PR_UKE: Int?
) {

	fun mapTiltakDeltaker(): TiltakDeltaker {
		val tiltakdeltakerId = TILTAKDELTAKER_ID.toString().also {
			if (it == "0") throw ValidationException("TILTAKDELTAKER_ID er 0")
		}

		val tiltakgjennomforingId = TILTAKGJENNOMFORING_ID.toString().also {
			if (it == "0") throw ValidationException("TILTAKGJENNOMFORING_ID er 0")
		}

		return TiltakDeltaker(
			tiltakdeltakerId = tiltakdeltakerId,
			tiltakgjennomforingId = tiltakgjennomforingId,
			personId = PERSON_ID?.toString() ?: throw ValidationException("PERSON_ID er null"),
			datoFra = DATO_FRA?.asValidatedLocalDate("DATO_FRA"),
			datoTil = DATO_TIL?.asValidatedLocalDate("DATO_TIL"),
			deltakerStatusKode = DELTAKERSTATUSKODE,
			datoStatusendring = DATO_STATUSENDRING?.asValidatedLocalDate("DATO_STATUSENDRING"),
			dagerPerUke = ANTALL_DAGER_PR_UKE,
			prosentDeltid = PROSENT_DELTID,
			regDato = REG_DATO?.asValidatedLocalDateTime("REG_DATO")
		)
	}

}
// @SONAR_STOP@

data class TiltakDeltaker(
	val tiltakdeltakerId: String,
	val tiltakgjennomforingId: String,
	val personId: String,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val deltakerStatusKode: String,
	val datoStatusendring: LocalDate?,
	val dagerPerUke: Int?,
	val prosentDeltid: Float?,
	val regDato: LocalDateTime?
)

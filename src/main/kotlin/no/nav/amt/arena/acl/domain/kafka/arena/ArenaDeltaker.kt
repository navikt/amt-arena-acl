package no.nav.amt.arena.acl.domain.kafka.arena

import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.repositories.DeltakerInsertDbo
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.asValidatedLocalDate
import no.nav.amt.arena.acl.utils.asValidatedLocalDateTime
import java.util.UUID

// @SONAR_START@
data class ArenaDeltaker(
	val TILTAKDELTAKER_ID: Long,
	val PERSON_ID: Long? = null,
	val TILTAKGJENNOMFORING_ID: Long,
	val DELTAKERSTATUSKODE: String,
	val DELTAKERTYPEKODE: String? = null,
	val AARSAKVERDIKODE_STATUS: String? = null,
	val OPPMOTETYPEKODE: String? = null,
	val PRIORITET: Int? = null,
	val BEGRUNNELSE_INNSOKT: String? = null,
	val BEGRUNNELSE_PRIORITERING: String? = null,
	val REG_DATO: String,
	val REG_USER: String? = null,
	val MOD_DATO: String,
	val MOD_USER: String? = null,
	val DATO_SVARFRIST: String? = null,
	val DATO_FRA: String? = null,
	val DATO_TIL: String? = null,
	val BEGRUNNELSE_STATUS: String? = null,
	val PROSENT_DELTID: Float? = null,
	val BRUKERID_STATUSENDRING: String,
	val DATO_STATUSENDRING: String? = null,
	val AKTIVITET_ID: Long,
	val BRUKERID_ENDRING_PRIORITERING: String? = null,
	val DATO_ENDRING_PRIORITERING: String? = null,
	val DOKUMENTKODE_SISTE_BREV: String? = null,
	val STATUS_INNSOK_PAKKE: String? = null,
	val STATUS_OPPTAK_PAKKE: String? = null,
	val OPPLYSNINGER_INNSOK: String? = null,
	val PARTISJON: Int? = null,
	val BEGRUNNELSE_BESTILLING: String? = null,
	val ANTALL_DAGER_PR_UKE: Float? = null,
	val EKSTERN_ID: String? = null
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
			deltakerStatusKode = TiltakDeltaker.Status.valueOf(DELTAKERSTATUSKODE),
			datoStatusendring = DATO_STATUSENDRING?.asValidatedLocalDateTime("DATO_STATUSENDRING"),
			statusAarsakKode = AARSAKVERDIKODE_STATUS?.let { TiltakDeltaker.StatusAarsak.valueOf(AARSAKVERDIKODE_STATUS) },
			dagerPerUke = ANTALL_DAGER_PR_UKE,
			prosentDeltid = PROSENT_DELTID,
			regDato = REG_DATO.asValidatedLocalDateTime("REG_DATO"),

			// I tiltaksarrangor-bff så er det ikke mulig å slette bestillingsteksten ved å sette den til null,
			// så for å fjerne tekst som finnes må man sette en tom string i steden for null
			innsokBegrunnelse = BEGRUNNELSE_BESTILLING ?: ""
		)
	}

	fun toDbo() = DeltakerInsertDbo(
		arenaId = TILTAKDELTAKER_ID,
		personId = PERSON_ID,
		gjennomforingId = TILTAKGJENNOMFORING_ID,
		datoFra = DATO_FRA?.asValidatedLocalDate("DATO_FRA"),
		datoTil = DATO_TIL?.asValidatedLocalDate("DATO_TIL"),
		regDato = REG_DATO.asValidatedLocalDateTime("REG_DATO"),
		modDato = MOD_DATO.asValidatedLocalDateTime("MOD_DATO"),
		status = DELTAKERSTATUSKODE,
		datoStatusEndring = DATO_STATUSENDRING?.asValidatedLocalDateTime("DATO_STATUSENDRING"),
		arenaSourceTable = ARENA_DELTAKER_TABLE_NAME,
		eksternId = EKSTERN_ID?.let { UUID.fromString(EKSTERN_ID)},
	)

}
// @SONAR_STOP@

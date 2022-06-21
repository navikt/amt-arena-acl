package no.nav.amt.arena.acl.domain.kafka.arena

import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.utils.asValidatedLocalDate
import no.nav.amt.arena.acl.utils.asValidatedLocalDateTime
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

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
	val REG_DATO: String? = null,
	val REG_USER: String? = null,
	val MOD_DATO: String? = null,
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
	val ANTALL_DAGER_PR_UKE: Int? = null
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val placeholderDate = LocalDateTime.of(1970, Month.JANUARY, 1, 0,0)

	fun mapTiltakDeltaker(): TiltakDeltaker {
		val tiltakdeltakerId = TILTAKDELTAKER_ID.toString().also {
			if (it == "0") throw ValidationException("TILTAKDELTAKER_ID er 0")
		}

		val tiltakgjennomforingId = TILTAKGJENNOMFORING_ID.toString().also {
			if (it == "0") throw ValidationException("TILTAKGJENNOMFORING_ID er 0")
		}

		val regDato = REG_DATO?.asValidatedLocalDateTime("REG_DATO") ?: placeholderDate.also {
				log.warn("Bruker med arenaId=${tiltakdeltakerId} mangler REG_DATO, bruker placeholder dato istedenfor")
			}


		return TiltakDeltaker(
			tiltakdeltakerId = tiltakdeltakerId,
			tiltakgjennomforingId = tiltakgjennomforingId,
			personId = PERSON_ID?.toString() ?: throw ValidationException("PERSON_ID er null"),
			datoFra = DATO_FRA?.asValidatedLocalDate("DATO_FRA"),
			datoTil = DATO_TIL?.asValidatedLocalDate("DATO_TIL"),
			deltakerStatusKode = DELTAKERSTATUSKODE,
			datoStatusendring = DATO_STATUSENDRING?.asValidatedLocalDateTime("DATO_STATUSENDRING"),
			dagerPerUke = ANTALL_DAGER_PR_UKE,
			prosentDeltid = PROSENT_DELTID,
			regDato = regDato,
			innsokBegrunnelse = BEGRUNNELSE_BESTILLING
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
	val datoStatusendring: LocalDateTime?,
	val dagerPerUke: Int?,
	val prosentDeltid: Float?,
	val regDato: LocalDateTime,
	val innsokBegrunnelse: String?
)

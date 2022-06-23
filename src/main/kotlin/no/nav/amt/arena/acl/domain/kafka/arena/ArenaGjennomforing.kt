package no.nav.amt.arena.acl.domain.kafka.arena

import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.utils.asValidatedLocalDate
import no.nav.amt.arena.acl.utils.asValidatedLocalDateTime
import no.nav.amt.arena.acl.utils.validatedLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime

// @SONAR_START@
data class ArenaGjennomforing(
	val TILTAKGJENNOMFORING_ID: Long,
	val SAK_ID: Long? = null,
	val TILTAKSKODE: String,
	val ANTALL_DELTAKERE: Int? = null,
	val ANTALL_VARIGHET: Int? = null,
	val DATO_FRA: String? = null,
	val DATO_TIL: String? = null,
	val FAGPLANKODE: String? = null,
	val MAALEENHET_VARIGHET: String? = null,
	val TEKST_FAGBESKRIVELSE: String? = null,
	val TEKST_KURSSTED: String? = null,
	val TEKST_MAALGRUPPE: String? = null,
	val STATUS_TREVERDIKODE_INNSOKNING: String? = null,
	val REG_DATO: String? = null,
	val REG_USER: String? = null,
	val MOD_DATO: String? = null,
	val MOD_USER: String? = null,
	val LOKALTNAVN: String? = null,
	val TILTAKSTATUSKODE: String? = null,
	val PROSENT_DELTID: Float? = null,
	val KOMMENTAR: String? = null,
	val ARBGIV_ID_ARRANGOR: Long? = null,
	val PROFILELEMENT_ID_GEOGRAFI: String? = null,
	val KLOKKETID_FREMMOTE: String? = null,
	val DATO_FREMMOTE: String? = null,
	val BEGRUNNELSE_STATUS: String? = null,
	val AVTALE_ID: Long? = null,
	val AKTIVITET_ID: Long? = null,
	val DATO_INNSOKNINGSTART: String? = null,
	val GML_FRA_DATO: String? = null,
	val GML_TIL_DATO: String? = null,
	val AETAT_FREMMOTEREG: String? = null,
	val AETAT_KONTERINGSSTED: String? = null,
	val OPPLAERINGNIVAAKODE: String? = null,
	val TILTAKGJENNOMFORING_ID_REL: String? = null,
	val VURDERING_GJENNOMFORING: String? = null,
	val PROFILELEMENT_ID_OPPL_TILTAK: String? = null,
	val DATO_OPPFOLGING_OK: String? = null,
	val PARTISJON: Long? = null,
	val MAALFORM_KRAVBREV: String? = null
) {
	fun mapTiltakGjennomforing(): TiltakGjennomforing {
		return TiltakGjennomforing(
			tiltakgjennomforingId = TILTAKGJENNOMFORING_ID.toString(),
			sakId = SAK_ID ?: throw ValidationException("SAK_ID er null"),
			tiltakskode = TILTAKSKODE,
			arbgivIdArrangor = ARBGIV_ID_ARRANGOR?.toString()
				?: throw ValidationException("ARBGIV_ID_ARRANGOR er null"),
			lokaltNavn = LOKALTNAVN ?: throw ValidationException("LOKALTNAVN er null"),
			datoFra = DATO_FRA?.asValidatedLocalDate("DATO_FRA"),
			datoTil = DATO_TIL?.asValidatedLocalDate("DATO_TIL"),
			datoFremmote = DATO_FREMMOTE?.validatedLocalDateTime(
				"DATO_FREMMOTE + KLOKKETID_FREMMOTE",
				KLOKKETID_FREMMOTE
			),
			tiltakstatusKode = TILTAKSTATUSKODE
				?: throw ValidationException("Forventet at TILTAKSTATUSKODE ikke er null"),
			regDato = REG_DATO?.asValidatedLocalDateTime("REG_DATO")
				?: throw ValidationException("REG_DATO er null")
		)
	}
}
// @SONAR_STOP@

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
	val SAK_ID: Long?,
	val TILTAKSKODE: String,
	val ANTALL_DELTAKERE: Int?,
	val ANTALL_VARIGHET: Int?,
	val DATO_FRA: String?,
	val DATO_TIL: String?,
	val FAGPLANKODE: String?,
	val MAALEENHET_VARIGHET: String?,
	val TEKST_FAGBESKRIVELSE: String?,
	val TEKST_KURSSTED: String?,
	val TEKST_MAALGRUPPE: String?,
	val STATUS_TREVERDIKODE_INNSOKNING: String?,
	val REG_DATO: String?,
	val REG_USER: String?,
	val MOD_DATO: String?,
	val MOD_USER: String?,
	val LOKALTNAVN: String?,
	val TILTAKSTATUSKODE: String?,
	val PROSENT_DELTID: Float?,
	val KOMMENTAR: String?,
	val ARBGIV_ID_ARRANGOR: Long?,
	val PROFILELEMENT_ID_GEOGRAFI: String?,
	val KLOKKETID_FREMMOTE: String?,
	val DATO_FREMMOTE: String?,
	val BEGRUNNELSE_STATUS: String?,
	val AVTALE_ID: Long?,
	val AKTIVITET_ID: Long?,
	val DATO_INNSOKNINGSTART: String?,
	val GML_FRA_DATO: String?,
	val GML_TIL_DATO: String?,
	val AETAT_FREMMOTEREG: String?,
	val AETAT_KONTERINGSSTED: String?,
	val OPPLAERINGNIVAAKODE: String?,
	val TILTAKGJENNOMFORING_ID_REL: String?,
	val VURDERING_GJENNOMFORING: String?,
	val PROFILELEMENT_ID_OPPL_TILTAK: String?,
	val DATO_OPPFOLGING_OK: String?,
	val PARTISJON: Long?,
	val MAALFORM_KRAVBREV: String?
) {
	fun mapTiltakGjennomforing(): TiltakGjennomforing {
		return TiltakGjennomforing(
			tiltakgjennomforingId = TILTAKGJENNOMFORING_ID.toString(),
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

data class TiltakGjennomforing(
	val tiltakgjennomforingId: String,
	val tiltakskode: String,
	val arbgivIdArrangor: String,
	val lokaltNavn: String,
	val datoFra: LocalDate?,
	val datoTil: LocalDate?,
	val datoFremmote: LocalDateTime?,
	val tiltakstatusKode: String,
	val regDato: LocalDateTime
)

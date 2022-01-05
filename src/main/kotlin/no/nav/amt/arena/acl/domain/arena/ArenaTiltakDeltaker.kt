package no.nav.amt.arena.acl.domain.arena

// @SONAR_START@
data class ArenaTiltakDeltaker(
	val TILTAKDELTAKER_ID: Long,
	val PERSON_ID: Long,
	val TILTAKGJENNOMFORING_ID: Long,
	val DELTAKERSTATUSKODE: String,
	val DELTAKERTYPEKODE: String,
	val AARSAKVERDIKODE_STATUS: String?,
	val OPPMOTETYPEKODE: String?,
	val PRIORITET: Int?,
	val BEGRUNNELSE_INNSOKT: String?,
	val BEGRUNNELSE_PRIORITERING: String?,
	val REG_DATO: String,
	val REG_USER: String,
	val MOD_DATO: String,
	val MOD_USER: String,
	val DATO_SVARFRIST: String?,
	val DATO_FRA: String?,
	val DATO_TIL: String?,
	val BEGRUNNELSE_STATUS: String?,
	val PROSENT_DELTID: Float,
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
)
// @SONAR_STOP@

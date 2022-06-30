package no.nav.amt.arena.acl.domain.kafka.arena

import no.nav.amt.arena.acl.exceptions.ValidationException

// @SONAR_START@
data class ArenaSak(
	val SAK_ID: Long,
	val SAKSKODE: String,
	val REG_DATO: String,
	val REG_USER: String,
	val MOD_DATO: String,
	val MOD_USER: String,
	val TABELLNAVNALIAS: String,
	val OBJEKT_ID: Long?,
	val AAR: Int,
	val LOPENRSAK: Int,
	val DATO_AVSLUTTET: String?,
	val SAKSTATUSKODE: String,
	val ARKIVNOKKEL: Long?,
	val AETATENHET_ARKIV: String?,
	val ARKIVHENVISNING: String?,
	val BRUKERID_ANSVARLIG: String?,
	val AETATENHET_ANSVARLIG: String?,
	val OBJEKT_KODE: String?,
	val STATUS_ENDRET: String?,
	val PARTISJON: Long?,
	val ER_UTLAND: String,
) {
	fun mapSak(): Sak {
		return Sak(
			sakId = SAK_ID,
			sakskode = SAKSKODE,
			aar = AAR,
			lopenr = LOPENRSAK,
			ansvarligEnhetId = AETATENHET_ANSVARLIG ?: throw ValidationException("Sak mangler \"AETATENHET_ANSVARLIG\"")
		)
	}
}
// @SONAR_STOP@


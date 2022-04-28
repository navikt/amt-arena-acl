package no.nav.amt.arena.acl.domain.kafka.arena

// @SONAR_START@
data class ArenaSak(
	val SAK_ID: Long,
	val SAKSKODE: String,
	val REG_DATO: String, // "2021-07-01 10:16:13"
	val REG_USER: String,
	val MOD_DATO: String, // "2022-02-02 00:00:00"
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
	val BRUKERID_ANSVARLIG: String,
	val AETATENHET_ANSVARLIG: String,
	val OBJEKT_KODE: Long?,
	val STATUS_ENDRET: String, // "2022-02-02 00:00:00"
	val PARTISJON: Long?,
	val ER_UTLAND: String,
) {
	fun mapSak(): Sak {
		return Sak(
			sakId = SAK_ID,
			aar = AAR,
			lopenr = LOPENRSAK,
			ansvarligEnhetId = AETATENHET_ANSVARLIG
		)
	}
}
// @SONAR_STOP@

data class Sak(
	val sakId: Long,
	val aar: Int,
	val lopenr: Int,
	val ansvarligEnhetId: String,
)

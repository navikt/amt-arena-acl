package no.nav.amt.arena.acl.domain.kafka.arena


data class Sak(
	val sakId: Long,
	val sakskode: String,
	val aar: Int,
	val lopenr: Int,
	val ansvarligEnhetId: String,
)

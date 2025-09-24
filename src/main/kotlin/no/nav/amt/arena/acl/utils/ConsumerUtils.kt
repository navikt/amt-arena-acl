package no.nav.amt.arena.acl.utils

val SUPPORTED_TILTAK = setOf(
	"INDOPPFAG",
	"ARBFORB",
	"AVKLARAG",
	"VASV",
	"ARBRRHDAG",
	"DIGIOPPARB",
	"JOBBK",
	"GRUPPEAMO",
	"GRUFAGYRKE"
)

val enkeltPlassTiltakskoder = setOf(
	"ENKELAMO",
	"ENKFAGYRKE",
	"HOYEREUTD"
)

fun isSupportedTiltak(kode: String): Boolean = SUPPORTED_TILTAK
	.plus(enkeltPlassTiltakskoder)
	.contains(kode)

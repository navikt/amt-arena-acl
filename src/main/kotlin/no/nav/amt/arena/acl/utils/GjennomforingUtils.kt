package no.nav.amt.arena.acl.utils

import no.nav.amt.arena.acl.domain.Gjennomforing
import no.nav.amt.arena.acl.repositories.GjennomforingDbo

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

fun GjennomforingDbo.toModel() = Gjennomforing(
	arenaId = arenaId,
	tiltakKode = tiltakKode,
	isValid = isValid,
	id = id
)

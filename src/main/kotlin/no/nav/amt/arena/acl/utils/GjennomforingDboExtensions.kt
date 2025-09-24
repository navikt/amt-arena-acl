package no.nav.amt.arena.acl.utils

import no.nav.amt.arena.acl.domain.Gjennomforing
import no.nav.amt.arena.acl.repositories.GjennomforingDbo


fun GjennomforingDbo.toModel() = Gjennomforing(
	arenaId = arenaId,
	tiltakKode = tiltakKode,
	isValid = isValid,
	id = id
)

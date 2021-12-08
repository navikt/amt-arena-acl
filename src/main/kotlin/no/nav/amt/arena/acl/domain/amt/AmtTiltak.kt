package no.nav.amt.arena.acl.domain.amt

import no.nav.amt.arena.acl.domain.arena.ArenaTiltak
import java.util.*

data class AmtTiltak(
	val id: UUID,
	val kode: String,
	val navn: String
) : AmtPayload {
}

class AmtTiltakMapper {

	companion object {
		fun map(arenaTiltak: ArenaTiltak): AmtTiltak {
			return AmtTiltak(
				id = UUID.randomUUID(),
				kode = arenaTiltak.TILTAKSKODE,
				navn = arenaTiltak.TILTAKSNAVN
			)
		}
	}

}

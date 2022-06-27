package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import java.time.LocalDateTime

interface DeltakerStatusProvider {
	fun getStatus () : AmtDeltaker.Status
	fun getEndretDato () : LocalDateTime?
}

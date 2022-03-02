package no.nav.amt.arena.acl.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.arena.ArenaTiltakDeltaker
import org.springframework.stereotype.Component

@Component
class DeltakerMetricHandler(
	private val registry: MeterRegistry
) {

	fun publishMetrics(data: ArenaData) {
		if (data.operation == AmtOperation.CREATED) {
			registry.counter("amt.arena-acl.deltaker.ny").increment()
		} else if (data.operation == AmtOperation.MODIFIED) {
			val before = data.jsonObject<ArenaTiltakDeltaker>(data.before)
			val after = data.jsonObject<ArenaTiltakDeltaker>(data.after)

			if (before?.DATO_FRA != after?.DATO_FRA) {
				registry.counter(
					"amt.arena-acl.deltaker.oppdatering",
					listOf(Tag.of("field", "startDato"))
				).increment()
			}

			if (before?.DATO_TIL != after?.DATO_TIL) {
				registry.counter(
					"amt.arena-acl.deltaker.oppdatering",
					listOf(Tag.of("field", "sluttDato"))
				).increment()
			}
		}
	}
}

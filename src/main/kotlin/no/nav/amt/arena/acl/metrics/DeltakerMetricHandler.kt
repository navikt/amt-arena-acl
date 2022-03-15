package no.nav.amt.arena.acl.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import org.springframework.stereotype.Component

@Component
class DeltakerMetricHandler(
	private val registry: MeterRegistry
) {

	fun publishMetrics(message: ArenaDeltakerKafkaMessage) {
		if (message.operationType == AmtOperation.CREATED) {
			registry.counter("amt.arena-acl.deltaker.ny").increment()
		} else if (message.operationType == AmtOperation.MODIFIED) {
			val before = message.before
			val after = message.after

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

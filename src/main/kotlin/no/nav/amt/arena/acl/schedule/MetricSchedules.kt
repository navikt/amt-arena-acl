package no.nav.amt.arena.acl.schedule

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ONE_MINUTE
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
open class MetricSchedules(
	private val arenaDataRepository: ArenaDataRepository,
	private val meterRegistry: MeterRegistry,
) {
	private val ingestStatusGaugeName = "amt.arena-acl.ingest.status"
	private val log = LoggerFactory.getLogger(javaClass)

	private fun createGauge(status: String) = meterRegistry.gauge(
		ingestStatusGaugeName, Tags.of("status", status), AtomicInteger(0))

	private var statusGauges: Map<String, AtomicInteger> = IngestStatus.values().associate {
		it.name to createGauge(it.name)!!
	}

	@Scheduled(fixedDelay = ONE_MINUTE, initialDelay = ONE_MINUTE)
	fun logIngestStatus() {
		log.info("Collecting metrics for ingest status")
		arenaDataRepository.getStatusCount()
			.forEach { status -> statusGauges.getValue(status.status.name).set(status.count) }
	}

}

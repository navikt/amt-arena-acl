package no.nav.amt.arena.acl.schedule

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ONE_MINUTE
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
open class MetricShedules(
	private val arenaDataRepository: ArenaDataRepository,
	private val meterRegistry: MeterRegistry,
) {

	@Scheduled(fixedDelay = ONE_MINUTE, initialDelay = ONE_MINUTE)
	open fun logArenaDataStatuses() {
		val gaugeName = "amt.arena-acl.ingest.status"

		arenaDataRepository.getStatusCount()
			.groupBy { it.table }
			.forEach { (tableName, statusList) ->
				val countByStatus = statusList.associateBy { it.status }

				IngestStatus.values().forEach { status ->

					meterRegistry.gauge(
						gaugeName,
						listOf(
							Tag.of("table", tableName),
							Tag.of("status", status.name)
						),
						countByStatus[status]?.count ?: 0
					)
				}

			}
	}

}

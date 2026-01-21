package no.nav.amt.arena.acl.schedule

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.FIVE_MINUTES
import no.nav.amt.arena.acl.utils.ONE_MINUTE
import no.nav.common.job.leader_election.LeaderElectionClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class MetricSchedules(
	private val arenaDataRepository: ArenaDataRepository,
	private val leaderElectionClient: LeaderElectionClient,
	meterRegistry: MeterRegistry,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	private val failedGauge: AtomicInteger = meterRegistry.gauge(
		INGEST_STATUS_GAUGE_NAME,
		Tags.of(STATUS_KEY, IngestStatus.FAILED.name),
		AtomicInteger(0)
	)!!

	@Scheduled(fixedDelay = FIVE_MINUTES, initialDelay = ONE_MINUTE)
	fun logIngestStatus() {
		if (leaderElectionClient.isLeader) {
			log.debug("Collecting metrics for FAILED ingest status")
			failedGauge.set(arenaDataRepository.getFailedIngestStatusCount())
		}
	}

	companion object {
		private const val INGEST_STATUS_GAUGE_NAME = "amt.arena-acl.ingest.status"
		private const val STATUS_KEY = "status"
	}
}

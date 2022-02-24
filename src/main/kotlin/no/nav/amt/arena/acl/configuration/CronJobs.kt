package no.nav.amt.arena.acl.configuration

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.ArenaMessageProcessorService
import no.nav.common.job.JobRunner
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler


@Configuration
@EnableScheduling
open class CronJobs(
	private val messageProcessorService: ArenaMessageProcessorService,
	private val arenaDataRepository: ArenaDataRepository,
	private val meterRegistry: MeterRegistry,
) {

	companion object {
		private const val ONE_MINUTE = 60 * 1000L
		private const val ONE_HOUR = 60 * 60 * 1000L
		private const val AT_MIDNIGHT = "0 0 0 * * *"
	}

	private val log = LoggerFactory.getLogger(javaClass)

	@Bean
	open fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler {
		val threadPoolTaskScheduler = ThreadPoolTaskScheduler()
		threadPoolTaskScheduler.poolSize = 3
		return threadPoolTaskScheduler
	}

	@Scheduled(fixedDelay = 15 * ONE_MINUTE, initialDelay = ONE_MINUTE)
	open fun processArenaMessages() {
		JobRunner.run("process_arena_messages", messageProcessorService::processMessages)
	}

	@Scheduled(cron = AT_MIDNIGHT)
	open fun processFailedArenaMessages() {
		JobRunner.run("process_failed_arena_messages", messageProcessorService::processFailedMessages)
	}

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

	@Scheduled(fixedDelay = ONE_HOUR, initialDelay = ONE_MINUTE)
	open fun deleteIgnoredArenaData() {
		JobRunner.run("delete_ignored_data") {
			val rowsDeleted = arenaDataRepository.deleteAllIgnoredData()
			log.info("Slettet ignorert data fra arena_data rows=${rowsDeleted}")
		}
	}

}

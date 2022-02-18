package no.nav.amt.arena.acl.configuration

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.ArenaMessageProcessorService
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

	private val logger = LoggerFactory.getLogger(javaClass)

	@Bean
	open fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler {
		val threadPoolTaskScheduler = ThreadPoolTaskScheduler()
		threadPoolTaskScheduler.poolSize = 3
		return threadPoolTaskScheduler
	}

	@Scheduled(fixedDelay = 60 * 1000, initialDelay = 60 * 1000) // Hvert minutt
	open fun processArenaMessages() {
		logger.debug("Starting processing job for uningested Arena Data...")
		messageProcessorService.processMessages()
		logger.debug("Finished processing job for uningested Arena Data!")
	}

	@Scheduled(cron = "0 0 0 * * *") // Hver dag ved midnatt
	open fun processFailedMessages() {
		logger.debug("Starting processing job for failed Arena Data...")
		messageProcessorService.processFailedMessages()
		logger.debug("Finished processing job for failed Arena Data!")
	}

	@Scheduled(fixedDelay = 20 * 1000, initialDelay = 60 * 1000)
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

	@Scheduled(cron = "0 0 * * * *") // Hver time
	open fun deleteIgnoredArenaData() {
		val rowsDeleted = arenaDataRepository.deleteAllIgnoredData()
		logger.info("Slettet ignorert data fra arena_data rows=${rowsDeleted}")
	}

}

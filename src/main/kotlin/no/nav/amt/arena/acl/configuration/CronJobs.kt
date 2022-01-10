package no.nav.amt.arena.acl.configuration

import no.nav.amt.arena.acl.ArenaMessageProcessorService
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
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
	private val arenaDataRepository: ArenaDataRepository
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	@Bean
	open fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler? {
		val threadPoolTaskScheduler = ThreadPoolTaskScheduler()
		threadPoolTaskScheduler.poolSize = 3
		return threadPoolTaskScheduler
	}

	@Scheduled(cron = "0 * * * * *") // Hvert minutt
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

	@Scheduled(fixedRate = 20000)
	open fun logArenaDataStatuses() {
		arenaDataRepository.logStatus()
	}
}

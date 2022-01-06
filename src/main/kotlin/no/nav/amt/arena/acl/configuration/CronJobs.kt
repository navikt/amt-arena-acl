package no.nav.amt.arena.acl.configuration

import no.nav.amt.arena.acl.ArenaMessageProcessorService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
open class CronJobs(
	private val messageProcessorService: ArenaMessageProcessorService
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	@Scheduled(cron = "0 * * * * *")
	open fun processArenaMessages() {
		logger.info("Starting processing job for uningested Arena Data...")
		messageProcessorService.processMessages()
		logger.info("Finished processing job for uningested Arena Data!")
	}
}

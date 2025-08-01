package no.nav.amt.arena.acl.configuration

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@EnableScheduling
@Configuration(proxyBeanMethods = false)
class ScheduleConfig {
	@Bean
	fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler =
		ThreadPoolTaskScheduler().apply {
			poolSize = 3
		}

	@Bean
	fun leaderElectionClient(lockProvider: LockProvider): LeaderElectionClient = ShedLockLeaderElectionClient(lockProvider)

	@Bean
	fun lockProvider(jdbcTemplate: JdbcTemplate): LockProvider =
		JdbcTemplateLockProvider(
			JdbcTemplateLockProvider.Configuration
				.builder()
				.withJdbcTemplate(jdbcTemplate)
				.usingDbTime()
				.build(),
		)
}

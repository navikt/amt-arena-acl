package no.nav.amt.arena.acl.integration

import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.integration.executors.DeltakerTestExecutor
import no.nav.amt.arena.acl.integration.executors.GjennomforingTestExecutor
import no.nav.amt.arena.acl.integration.executors.SakTestExecutor
import no.nav.amt.arena.acl.integration.executors.TiltakTestExecutor
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.integration.kafka.SingletonKafkaProvider
import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.mocks.MockArenaOrdsProxyHttpServer
import no.nav.amt.arena.acl.mocks.MockMachineToMachineHttpServer
import no.nav.amt.arena.acl.mocks.MockMrArenaAdapterServer
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.ArenaSakRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.services.RetryArenaMessageProcessorService
import no.nav.amt.arena.acl.services.TiltakService
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import javax.sql.DataSource

@SpringBootTest
@Import(IntegrationTestConfiguration::class)
@ActiveProfiles("integration")
@TestConfiguration("application-integration.properties")
// Når toggle fjernes kan DirtiesContext også fjernes
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
abstract class IntegrationTestBase {

	@Autowired
	lateinit var dataSource: DataSource

	@Autowired
	private lateinit var retryArenaMessageProcessorService: RetryArenaMessageProcessorService

	@Autowired
	lateinit var tiltakService: TiltakService

	@Autowired
	lateinit var tiltakExecutor: TiltakTestExecutor

	@Autowired
	lateinit var gjennomforingExecutor: GjennomforingTestExecutor

	@Autowired
	lateinit var sakExecutor: SakTestExecutor

	@Autowired
	lateinit var deltakerExecutor: DeltakerTestExecutor

	@BeforeEach
	fun beforeEach() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@AfterEach
	fun cleanup() {
		tiltakService.invalidateTiltakByKodeCache()
		mockArenaOrdsProxyHttpServer.reset()
		mockMrArenaAdapterServer.reset()
	}

	companion object {
		val mockMachineToMachineHttpServer = MockMachineToMachineHttpServer()
		val mockMrArenaAdapterServer = MockMrArenaAdapterServer()
		val mockArenaOrdsProxyHttpServer = MockArenaOrdsProxyHttpServer()

		@JvmStatic
		@DynamicPropertySource
		fun startEnvironment(registry: DynamicPropertyRegistry) {
            setupEnvironment(registry)
		}

		fun setupEnvironment(registry: DynamicPropertyRegistry) {
			mockMrArenaAdapterServer.start()
			registry.add("mr-arena-adapter.url") { mockMrArenaAdapterServer.serverUrl() }
			registry.add("mr-arena-adapter.scope") { "test.mr-arena-adapter" }


			mockArenaOrdsProxyHttpServer.start()
			registry.add("amt-arena-ords-proxy.scope") { "test.amt-arena-ords-proxy" }
			registry.add("amt-arena-ords-proxy.url") { mockArenaOrdsProxyHttpServer.serverUrl() }

			mockMachineToMachineHttpServer.start()
			registry.add("nais.env.azureOpenIdConfigTokenEndpoint") {
				mockMachineToMachineHttpServer.serverUrl() + MockMachineToMachineHttpServer.tokenPath
			}

			val container = SingletonPostgresContainer.getContainer()

			registry.add("spring.datasource.url") { container.jdbcUrl }
			registry.add("spring.datasource.username") { container.username }
			registry.add("spring.datasource.password") { container.password }
			registry.add("spring.datasource.hikari.maximum-pool-size") { 3 }

		}
	}

	fun processMessages(batchSize: Int = 500) {
		retryArenaMessageProcessorService.processMessages(batchSize)
	}
}

@Profile("integration")
@TestConfiguration
open class IntegrationTestConfiguration(
) {

	@Value("\${app.env.amtTopic}")
	lateinit var consumerTopic: String

	@Bean
	open fun tiltakExecutor(
		kafkaProducer: KafkaProducerClientImpl<String, String>,
		arenaDataRepository: ArenaDataRepository,
		translationRepository: ArenaDataIdTranslationRepository,
		tiltakRepository: TiltakRepository
	): TiltakTestExecutor {
		return TiltakTestExecutor(kafkaProducer, arenaDataRepository, translationRepository, tiltakRepository)
	}

	@Bean
	open fun gjennomforingExecutor(
		kafkaProducer: KafkaProducerClientImpl<String, String>,
		arenaDataRepository: ArenaDataRepository,
		translationRepository: ArenaDataIdTranslationRepository
	): GjennomforingTestExecutor {
		return GjennomforingTestExecutor(kafkaProducer, arenaDataRepository, translationRepository)
	}

	@Bean
	open fun sakExecutor(
		kafkaProducer: KafkaProducerClientImpl<String, String>,
		arenaDataRepository: ArenaDataRepository,
		translationRepository: ArenaDataIdTranslationRepository,
		sakRepository: ArenaSakRepository
	): SakTestExecutor {
		return SakTestExecutor(kafkaProducer, arenaDataRepository, translationRepository, sakRepository)
	}

	@Bean
	open fun deltakerExecutor(
		kafkaProducer: KafkaProducerClientImpl<String, String>,
		arenaDataRepository: ArenaDataRepository,
		translationRepository: ArenaDataIdTranslationRepository
	): DeltakerTestExecutor {
		return DeltakerTestExecutor(kafkaProducer, arenaDataRepository, translationRepository)
	}

	@Bean
	open fun kafkaProperties(): KafkaProperties {
		return SingletonKafkaProvider.getKafkaProperties()
	}

	@Bean
	open fun kafkaAmtIntegrationConsumer(properties: KafkaProperties): KafkaAmtIntegrationConsumer {
		return KafkaAmtIntegrationConsumer(properties, consumerTopic)
	}
}

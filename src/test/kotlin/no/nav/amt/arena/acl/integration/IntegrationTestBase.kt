package no.nav.amt.arena.acl.integration

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.integration.executors.DeltakerTestExecutor
import no.nav.amt.arena.acl.integration.executors.GjennomforingTestExecutor
import no.nav.amt.arena.acl.integration.executors.TiltakTestExecutor
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.integration.kafka.SingletonKafkaProvider
import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.ordsproxy.Arbeidsgiver
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.services.ArenaMessageProcessorService
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

@SpringBootTest
@Import(IntegrationTestConfiguration::class)
@ActiveProfiles("integration")
@TestConfiguration("application-integration.properties")
abstract class IntegrationTestBase {

	@Autowired
	lateinit var tiltakRepository: TiltakRepository

	@Autowired
	lateinit var dataSource: DataSource

	@Autowired
	private lateinit var messageProcessor: ArenaMessageProcessorService

	@Autowired
	lateinit var tiltakExecutor: TiltakTestExecutor

	@Autowired
	lateinit var gjennomforingExecutor: GjennomforingTestExecutor

	@Autowired
	lateinit var deltakerExecutor: DeltakerTestExecutor

	@BeforeEach
	fun beforeEach() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@AfterEach
	fun cleanup() {
		tiltakRepository.invalidateCache()
		IntegrationTestConfiguration.fnrHandlers.clear()
		IntegrationTestConfiguration.virksomhetsHandler.clear()
	}

	fun processMessages() {
		messageProcessor.processMessages()
	}
}

@TestConfiguration
open class IntegrationTestConfiguration(
) {

	companion object {
		val fnrHandlers = mutableMapOf<String, () -> String?>()
		val virksomhetsHandler = mutableMapOf<String, () -> String>()
	}

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
	open fun deltakerExecutor(
		kafkaProducer: KafkaProducerClientImpl<String, String>,
		arenaDataRepository: ArenaDataRepository,
		translationRepository: ArenaDataIdTranslationRepository
	): DeltakerTestExecutor {
		return DeltakerTestExecutor(kafkaProducer, arenaDataRepository, translationRepository)
	}

	@Bean
	open fun dataSource(): DataSource {
		return SingletonPostgresContainer.getDataSource()
	}

	@Bean
	open fun kafkaProperties(): KafkaProperties {
		return SingletonKafkaProvider.getKafkaProperties()
	}

	@Bean
	open fun kafkaConsumer(properties: KafkaProperties): KafkaAmtIntegrationConsumer {
		return KafkaAmtIntegrationConsumer(properties, consumerTopic)
	}

	@Bean
	open fun ordsProxyClient(): ArenaOrdsProxyClient {

		return object : ArenaOrdsProxyClient {
			override fun hentFnr(arenaPersonId: String): String? {
				if (fnrHandlers[arenaPersonId] != null) {
					return fnrHandlers[arenaPersonId]!!.invoke()
				}

				return "12345"

			}

			override fun hentArbeidsgiver(arenaArbeidsgiverId: String): Arbeidsgiver? {
				throw NotImplementedError()
			}

			override fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String {
				if (virksomhetsHandler[arenaArbeidsgiverId] != null) {
					return virksomhetsHandler[arenaArbeidsgiverId]!!.invoke()
				}

				return "12345"
			}

		}
	}
}

package no.nav.amt.arena.acl.integration

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.integration.kafka.SingletonKafkaProvider
import no.nav.amt.arena.acl.integration.utils.GjennomforingIntegrationTestHandler
import no.nav.amt.arena.acl.integration.utils.TiltakIntegrationTestHandler
import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.ordsproxy.Arbeidsgiver
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
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
	lateinit var kafkaProducerClientImpl: KafkaProducerClientImpl<String, String>

	@Autowired
	lateinit var arenaDataRepository: ArenaDataRepository

	@Autowired
	lateinit var tiltakRepository: TiltakRepository

	@Autowired
	lateinit var translationRepository: ArenaDataIdTranslationRepository

	@Autowired
	lateinit var consumer: KafkaAmtIntegrationConsumer

	@Autowired
	lateinit var dataSource: DataSource

	companion object {
		private var position = 0
	}

	@BeforeEach
	fun beforeEach() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@AfterEach
	fun cleanup() {
		KafkaAmtIntegrationConsumer.reset()
		tiltakRepository.invalidateCache()
	}

	fun getPosition(): String {
		return "${position++}"
	}

	fun tiltak(): TiltakIntegrationTestHandler {
		return TiltakIntegrationTestHandler(
			kafkaProducerClientImpl,
			arenaDataRepository,
			tiltakRepository
		)
	}

	fun gjennomforing(): GjennomforingIntegrationTestHandler {
		return GjennomforingIntegrationTestHandler(
			kafkaProducerClientImpl,
			arenaDataRepository,
			translationRepository
		)
	}
}

@TestConfiguration
open class IntegrationTestConfiguration(
) {

	@Value("\${app.env.amtTopic}")
	lateinit var consumerTopic: String

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

	//TODO Should be a WireMock instead
	@Bean
	open fun ordsProxyClient(): ArenaOrdsProxyClient {

		return object : ArenaOrdsProxyClient {
			override fun hentFnr(arenaPersonId: String): String? {
				return "12345"
			}

			override fun hentArbeidsgiver(arenaArbeidsgiverId: String): Arbeidsgiver? {
				return Arbeidsgiver("12345", "56789")
			}

			override fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String {
				return "12345"
			}

		}
	}
}

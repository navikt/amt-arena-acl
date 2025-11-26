package no.nav.amt.arena.acl.integration

import com.ninjasquad.springmockk.MockkBean
import io.getunleash.FakeUnleash
import net.javacrumbs.shedlock.core.LockProvider
import no.nav.amt.arena.acl.integration.IntegrationTestBase.Companion.kafkaContainer
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageConsumer
import no.nav.amt.arena.acl.integration.utils.MockOAuthServer
import no.nav.amt.arena.acl.kafka.KafkaConsumer
import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.amt.arena.acl.mocks.MockAmtTiltakServer
import no.nav.amt.arena.acl.mocks.MockArenaOrdsProxyHttpServer
import no.nav.amt.arena.acl.mocks.MockMachineToMachineHttpServer
import no.nav.amt.arena.acl.mocks.MockMulighetsrommetApiServer
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestConstructor
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.Properties
import java.util.concurrent.TimeUnit

@ActiveProfiles("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTestConfiguration::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class IntegrationTestBase(
	private val useKafkaConsumers: Boolean = true,
) : JUnitRepositoryTestBase() {
	@Autowired
	lateinit var kafkaMessageConsumer: KafkaMessageConsumer

	@Autowired
	private lateinit var kafkaConsumer: KafkaConsumer

	@MockkBean(relaxed = true)
	@Suppress("unused")
	private lateinit var mockLockProvider: LockProvider

	@BeforeEach
	fun beforeEach() {
		if (!useKafkaConsumers) return

		kafkaConsumer.start()
		kafkaMessageConsumer.start()
	}

	@AfterEach
	fun cleanup() {
		mockArenaOrdsProxyHttpServer.reset()
		mockMulighetsrommetApiServer.reset()
		mockAmtTiltakServer.reset()

		if (!useKafkaConsumers) return

		kafkaMessageConsumer.stop()
		kafkaMessageConsumer.reset()
		kafkaConsumer.stop()
	}

	init {
		Awaitility.setDefaultTimeout(10, TimeUnit.SECONDS)
	}

	companion object {
		val oAuthServer = MockOAuthServer()
		val mockMachineToMachineHttpServer = MockMachineToMachineHttpServer()
		val mockMulighetsrommetApiServer = MockMulighetsrommetApiServer()
		val mockArenaOrdsProxyHttpServer = MockArenaOrdsProxyHttpServer()
		val mockAmtTiltakServer = MockAmtTiltakServer()

		@ServiceConnection
		val kafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka")).apply {
			// workaround for https://github.com/testcontainers/testcontainers-java/issues/9506
			withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094")
			start()
			System.setProperty("KAFKA_BROKERS", bootstrapServers)
		}

		@JvmStatic
		@DynamicPropertySource
		@Suppress("unused")
		fun startEnvironment(registry: DynamicPropertyRegistry) {
			mockMulighetsrommetApiServer.start()
			oAuthServer.start()

			registry.add("no.nav.security.jwt.issuer.azuread.discovery-url", oAuthServer::getDiscoveryUrl)
			registry.add("no.nav.security.jwt.issuer.azuread.accepted-audience") { "test-aud" }
			registry.add("mulighetsrommet-api.url") { mockMulighetsrommetApiServer.serverUrl() }
			registry.add("mulighetsrommet-api.scope") { "test.mulighetsrommet-api" }

			mockArenaOrdsProxyHttpServer.start()
			registry.add("amt-arena-ords-proxy.scope") { "test.amt-arena-ords-proxy" }
			registry.add("amt-arena-ords-proxy.url") { mockArenaOrdsProxyHttpServer.serverUrl() }

			mockAmtTiltakServer.start()
			registry.add("amt-tiltak.scope") { "test.amt-tiltak" }
			registry.add("amt-tiltak.url") { mockAmtTiltakServer.serverUrl() }

			mockMachineToMachineHttpServer.start()
			registry.add("nais.env.azureOpenIdConfigTokenEndpoint") {
				mockMachineToMachineHttpServer.serverUrl() + MockMachineToMachineHttpServer.TOKEN_PATH
			}
		}
	}
}

@Profile("integration")
@TestConfiguration
class IntegrationTestConfiguration {

	@Bean
	fun kafkaProperties(): KafkaProperties {
		val host = kafkaContainer.bootstrapServers

		return object : KafkaProperties {
			override fun consumer(): Properties = KafkaPropertiesBuilder.consumerBuilder()
				.withBrokerUrl(host)
				.withBaseProperties()
				.withConsumerGroupId("INTEGRATION_CONSUMER")
				.withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
				.build()

			override fun producer(): Properties = KafkaPropertiesBuilder.producerBuilder()
				.withBrokerUrl(host)
				.withBaseProperties()
				.withProducerId("INTEGRATION_PRODUCER")
				.withSerializers(StringSerializer::class.java, StringSerializer::class.java)
				.build()
		}
	}

	@Bean
	fun kafkaAmtIntegrationConsumer(
		properties: KafkaProperties,
		@Value($$"${app.env.amtTopic}") consumerTopic: String,
	) = KafkaAmtIntegrationConsumer(
		kafkaProperties = properties,
		topic = consumerTopic,
	)

	@Bean
	fun unleashClient() = FakeUnleash().apply { enableAll() }
}

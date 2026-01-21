package no.nav.amt.arena.acl.integration

import io.getunleash.FakeUnleash
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.util.Properties

@Profile("integration")
@EnableJwtTokenValidation
@TestConfiguration(proxyBeanMethods = false)
class IntegrationTestConfiguration {
	@Bean
	@Primary
	fun kafkaProperties(): KafkaProperties {
		val host = IntegrationTestBase.kafkaContainer.bootstrapServers

		return object : KafkaProperties {
			override fun consumer(): Properties =
				KafkaPropertiesBuilder
					.consumerBuilder()
					.withBrokerUrl(host)
					.withBaseProperties()
					.withConsumerGroupId("INTEGRATION_CONSUMER")
					.withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
					.build()

			override fun tempConsumer(): Properties =
				KafkaPropertiesBuilder
					.consumerBuilder()
					.withBrokerUrl(host)
					.withBaseProperties()
					.withConsumerGroupId("INTEGRATION_TEMP_CONSUMER")
					.withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
					.build()

			override fun producer(): Properties =
				KafkaPropertiesBuilder
					.producerBuilder()
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

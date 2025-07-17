package no.nav.amt.arena.acl.kafka

import no.nav.common.kafka.producer.KafkaProducerClientImpl
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.Properties

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KafkaTopicProperties::class)
class KafkaConfiguration {
	@Value($$"${app.env.consumerId}")
	var consumerId: String = ""

	@Value($$"${app.env.producerId}")
	var producerId: String = ""

	@Bean
	fun kafkaProducer(kafkaProperties: KafkaProperties): KafkaProducerClientImpl<String, String> =
		KafkaProducerClientImpl(kafkaProperties.producer())

	@Bean
	@Profile("default")
	fun kafkaProperties(): KafkaProperties =
		object : KafkaProperties {
			override fun consumer(): Properties = KafkaPropertiesPreset.aivenDefaultConsumerProperties(consumerId)

			override fun producer(): Properties = KafkaPropertiesPreset.aivenDefaultProducerProperties(producerId)
		}

	@Bean
	@Profile("local")
	fun localKafkaProperties(): KafkaProperties =
		object : KafkaProperties {
			override fun consumer(): Properties =
				KafkaPropertiesBuilder
					.consumerBuilder()
					.withBrokerUrl("localhost:9092")
					.withBaseProperties()
					.withConsumerGroupId(consumerId)
					.withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
					.build()

			override fun producer(): Properties =
				KafkaPropertiesBuilder
					.producerBuilder()
					.withBrokerUrl(("localhost:9092"))
					.withBaseProperties()
					.withProducerId(producerId)
					.withSerializers(StringSerializer::class.java, StringSerializer::class.java)
					.build()
		}
}

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
import java.util.*

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties::class)
open class KafkaConfiguration {

	@Value("\${app.env.consumerId}")
	var consumerId: String = ""

	@Value("\${app.env.producerId}")
	var producerId: String = ""

	@Bean
	open fun kafkaProducer(kafkaProperties: KafkaProperties): KafkaProducerClientImpl<String, String> {
		return KafkaProducerClientImpl(kafkaProperties.producer())
	}

	@Bean
	@Profile("default")
	open fun kafkaProperties(): KafkaProperties {
		return object : KafkaProperties {
			override fun consumer(): Properties {
				return KafkaPropertiesPreset.aivenDefaultConsumerProperties(consumerId)
			}

			override fun producer(): Properties {
				return KafkaPropertiesPreset.aivenDefaultProducerProperties(producerId)
			}
		}
	}

	@Bean
	@Profile("local")
	open fun localKafkaProperties(): KafkaProperties {
		return object : KafkaProperties {
			override fun consumer(): Properties {
				return KafkaPropertiesBuilder.consumerBuilder()
					.withBrokerUrl("localhost:9092")
					.withBaseProperties()
					.withConsumerGroupId(consumerId)
					.withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
					.build()
			}

			override fun producer(): Properties {
				return KafkaPropertiesBuilder.producerBuilder()
					.withBrokerUrl(("localhost:9092"))
					.withBaseProperties()
					.withProducerId(producerId)
					.withSerializers(StringSerializer::class.java, StringSerializer::class.java)
					.build()
			}
		}
	}

}

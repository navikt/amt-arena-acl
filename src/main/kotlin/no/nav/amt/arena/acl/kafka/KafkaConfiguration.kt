package no.nav.amt.arena.acl.kafka

import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.*

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties::class)
open class KafkaConfiguration {

    @Bean
    @Profile("default")
    open fun kafkaConsumerProperties(): KafkaProperties {
        return object : KafkaProperties {
            override fun consumer(): Properties {
                return KafkaPropertiesPreset.aivenDefaultConsumerProperties("amt-tiltak-consumer")
            }

            override fun producer(): Properties {
                throw NotImplementedError("Not yet implemented")
            }
        }
    }

    @Bean
    @Profile("local")
    open fun localKafkaConsumerProperties(): KafkaProperties {
        return object : KafkaProperties {
            override fun consumer(): Properties {
                return KafkaPropertiesBuilder.consumerBuilder()
                    .withBrokerUrl("localhost:9092")
                    .withBaseProperties()
                    .withConsumerGroupId("amt-tiltak-consumer")
                    .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
                    .build()
            }

            override fun producer(): Properties {
                throw NotImplementedError("Not yet implemented")
            }
        }
    }

}

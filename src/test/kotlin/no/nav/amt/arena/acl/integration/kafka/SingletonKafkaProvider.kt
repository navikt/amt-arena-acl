package no.nav.amt.arena.acl.integration.kafka

import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

object SingletonKafkaProvider {

	private const val producerId = "INTEGRATION_PRODUCER"
	private const val consumerId = "INTEGRATION_CONSUMER"

	private val logger = LoggerFactory.getLogger(this::class.java)
	private const val kafkaDockerImageName = "confluentinc/cp-kafka:6.2.1"

	private var kafkaContainer: KafkaContainer? = null

	fun getKafkaProperties(): KafkaProperties {
		val host = getHost()

		val properties = object : KafkaProperties {
			override fun consumer(): Properties {
				return KafkaPropertiesBuilder.consumerBuilder()
					.withBrokerUrl(host)
					.withBaseProperties()
					.withConsumerGroupId(consumerId)
					.withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
					.build()
			}

			override fun producer(): Properties {
				return KafkaPropertiesBuilder.producerBuilder()
					.withBrokerUrl(host)
					.withBaseProperties()
					.withProducerId(producerId)
					.withSerializers(StringSerializer::class.java, StringSerializer::class.java)
					.build()
			}
		}

		return properties
	}

	private fun getHost(): String {
		if (kafkaContainer == null) {
			logger.info("Starting new Kafka Instance...")
			kafkaContainer = KafkaContainer(DockerImageName.parse(kafkaDockerImageName))
			kafkaContainer!!.start()
			setupShutdownHook()
		}
		return kafkaContainer!!.bootstrapServers
	}

	private fun setupShutdownHook() {
		Runtime.getRuntime().addShutdownHook(Thread {
			logger.info("Shutting down Kafka server...")
			kafkaContainer?.stop()
		})
	}


}
package no.nav.amt.arena.acl.integration.kafka

import no.nav.amt.arena.acl.kafka.KafkaProperties
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.Properties

object SingletonKafkaProvider {
	private const val PRODUCER_ID = "INTEGRATION_PRODUCER"
	private const val CONSUMER_ID = "INTEGRATION_CONSUMER"

	private val log = LoggerFactory.getLogger(javaClass)
	private var kafkaContainer: KafkaContainer? = null

	fun getKafkaProperties(): KafkaProperties {
		val host = getHost()

		val properties =
			object : KafkaProperties {
				override fun consumer(): Properties =
					KafkaPropertiesBuilder
						.consumerBuilder()
						.withBrokerUrl(host)
						.withBaseProperties()
						.withConsumerGroupId(CONSUMER_ID)
						.withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
						.build()

				override fun producer(): Properties =
					KafkaPropertiesBuilder
						.producerBuilder()
						.withBrokerUrl(host)
						.withBaseProperties()
						.withProducerId(PRODUCER_ID)
						.withSerializers(StringSerializer::class.java, StringSerializer::class.java)
						.build()
			}

		return properties
	}

	private fun getHost(): String {
		if (kafkaContainer == null) {
			log.info("Starting new Kafka Instance...")
			kafkaContainer =
				KafkaContainer(DockerImageName.parse("apache/kafka"))
					.withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094")
					// workaround for https://github.com/testcontainers/testcontainers-java/issues/9506
					.apply {
						start()
						System.setProperty("KAFKA_BROKERS", bootstrapServers)
					}
			setupShutdownHook()
		}
		return kafkaContainer!!.bootstrapServers
	}

	private fun setupShutdownHook() {
		Runtime.getRuntime().addShutdownHook(
			Thread {
				log.info("Shutting down Kafka server...")
				kafkaContainer?.stop()
			},
		)
	}
}

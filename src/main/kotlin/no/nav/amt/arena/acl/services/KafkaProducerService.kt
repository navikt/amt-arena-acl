package no.nav.amt.arena.acl.services

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.utils.JsonUtils.objectMapper
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KafkaProducerService(
	private val kafkaProducer: KafkaProducerClient<String, String>,
	@Value($$"${app.env.amtTopic}") private val amtTiltakTopic: String,
	@Value($$"${app.env.amtEnkeltplassDeltakerTopic}") private val amtEnkeltplassDeltakerTopic: String,
) {
	fun sendTilAmtTiltak(
		messageKey: UUID,
		data: AmtKafkaMessageDto<*>,
	) {
		val record = ProducerRecord(amtTiltakTopic, messageKey.toString(), objectMapper.writeValueAsString(data))
		kafkaProducer.sendSync(record)
	}

	fun produceEnkeltplassDeltaker(
		messageKey: UUID,
		deltaker: AmtDeltaker,
	) {
		val record =
			ProducerRecord(
				amtEnkeltplassDeltakerTopic,
				messageKey.toString(),
				objectMapper.writeValueAsString(deltaker),
			)
		kafkaProducer.sendSync(record)
	}

	fun tombstoneEnkeltplassDeltaker(messageKey: UUID) {
		val record: ProducerRecord<String, String?> =
			ProducerRecord(amtEnkeltplassDeltakerTopic, messageKey.toString(), null)
		kafkaProducer.sendSync(record)
	}
}

package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.arena.ArenaTiltakDeltaker
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.springframework.stereotype.Component

@Component
open class DeltakerProcessor(
	kafkaProducer: KafkaProducerClientImpl<String, String>
) : AbstractArenaProcessor<ArenaTiltakDeltaker>(
	clazz = ArenaTiltakDeltaker::class.java,
	kafkaProducer = kafkaProducer
) {

	override fun handle(data: ArenaData) {
		TODO("Not yet implemented")
	}

	private fun isIgnored(deltaker: ArenaTiltakDeltaker): Boolean {
		TODO("Not yet implemented")
	}
}

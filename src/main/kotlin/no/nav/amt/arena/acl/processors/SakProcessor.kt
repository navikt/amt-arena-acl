package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaSakKafkaMessage
import no.nav.amt.arena.acl.repositories.ArenaSakRepository
import org.springframework.stereotype.Component

@Component
open class SakProcessor(
	private val arenaSakRepository: ArenaSakRepository
) : ArenaMessageProcessor<ArenaSakKafkaMessage> {

	override fun handleArenaMessage(message: ArenaSakKafkaMessage) {
		val sak = message.getData().mapSak()

		arenaSakRepository.upsertSak(
			arenaSakId = sak.sakId,
			aar = sak.aar,
			lopenr = sak.lopenr,
			ansvarligEnhetId = sak.ansvarligEnhetId
		)
	}

}

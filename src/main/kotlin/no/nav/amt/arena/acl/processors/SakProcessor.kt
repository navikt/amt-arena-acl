package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaSakKafkaMessage
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.repositories.ArenaSakRepository
import org.springframework.stereotype.Component

@Component
open class SakProcessor(
	private val arenaSakRepository: ArenaSakRepository
) : ArenaMessageProcessor<ArenaSakKafkaMessage> {

	companion object {
		private const val SAKSKODE_TILTAK = "TILT"
	}

	override fun handleArenaMessage(message: ArenaSakKafkaMessage) {
		val sak = message.getData().mapSak()

		if (sak.sakskode != SAKSKODE_TILTAK) {
			throw IgnoredException("Sak med kode ${sak.sakskode} er ikke relevant")
		}

		arenaSakRepository.upsertSak(
			arenaSakId = sak.sakId,
			aar = sak.aar,
			lopenr = sak.lopenr,
			ansvarligEnhetId = sak.ansvarligEnhetId
		)
	}

}

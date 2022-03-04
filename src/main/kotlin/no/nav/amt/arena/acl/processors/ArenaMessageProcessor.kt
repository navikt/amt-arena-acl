package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage

interface ArenaMessageProcessor<M : ArenaKafkaMessage<*>> {

	fun handleArenaMessage(message: M)

}

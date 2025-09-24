package no.nav.amt.arena.acl.consumer

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage

fun interface ArenaMessageConsumer<M : ArenaKafkaMessage<*>> {
	fun handleArenaMessage(message: M)
}

package no.nav.amt.arena.acl.extensions

import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString

inline fun <reified T : Any> ArenaDataDbo.toArenaKafkaMessage(): ArenaKafkaMessage<T> =
	ArenaKafkaMessage(
		arenaTableName = arenaTableName,
		operationType = operation,
		operationTimestamp = operationTimestamp,
		operationPosition = operationPosition,
		before = before?.let { fromJsonString<T>(it) },
		after = after?.let { fromJsonString<T>(it) }
	)

package no.nav.amt.arena.acl.extensions

import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessage
import no.nav.amt.arena.acl.utils.JsonUtils.objectMapper
import tools.jackson.module.kotlin.readValue

inline fun <reified T : Any> ArenaDataDbo.toArenaKafkaMessage(): ArenaKafkaMessage<T> =
	ArenaKafkaMessage(
		arenaTableName = arenaTableName,
		operationType = operation,
		operationTimestamp = operationTimestamp,
		operationPosition = operationPosition,
		before = before?.let { objectMapper.readValue<T>(it) },
		after = after?.let { objectMapper.readValue<T>(it) },
	)

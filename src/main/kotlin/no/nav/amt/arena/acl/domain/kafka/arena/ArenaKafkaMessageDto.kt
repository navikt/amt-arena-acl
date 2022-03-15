package no.nav.amt.arena.acl.domain.kafka.arena

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

enum class ArenaOperation {
	I,
	U,
	D
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArenaKafkaMessageDto(
	val table: String,

	@JsonProperty("op_type")
	val opType: String,

	@JsonProperty("op_ts")
	val opTs: String,

	val pos: String,
	val before: JsonNode?,
	val after: JsonNode?
)

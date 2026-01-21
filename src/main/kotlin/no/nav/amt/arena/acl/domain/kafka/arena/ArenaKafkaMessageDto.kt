package no.nav.amt.arena.acl.domain.kafka.arena

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArenaKafkaMessageDto(
	val table: String,
	@JsonProperty("op_type")
	val opType: String,
	@JsonProperty("op_ts")
	val opTs: String,
	val pos: String,
	// Så lenge nøkkelen finnes når man deserialiserer en ArenaKafkaMessageDto
	// men er lik `null` så blir before / after satt til `NullNode` *ikke* `null`
	val before: JsonNode?,
	val after: JsonNode?,
)

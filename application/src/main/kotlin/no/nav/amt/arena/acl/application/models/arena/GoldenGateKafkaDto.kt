package no.nav.amt.arena.acl.application.models.arena

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

enum class ArenaOpType(val type: String) {
    I("I"),    // insert
    U("U"),    // update
    D("D"),    // delete
}

data class GoldenGateKafkaDto(
    val table: String,
    @JsonProperty("op_type") val operationType: ArenaOpType,
    @JsonProperty("op_ts") val operationTimestamp: String,
    @JsonProperty("current_ts") val currentTimestamp: String,
    val pos: String,
    val after: JsonNode?,
    val before: JsonNode?
)

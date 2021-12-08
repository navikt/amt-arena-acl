package no.nav.amt.arena.acl.application.domain.arena

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

enum class ArenaOperation {
    I,
    U,
    D
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArenaWrapper<T>(
    @JsonProperty("table_name")
    val table: String,

    @JsonProperty("op_type")
    val operation: ArenaOperation,

    val before: T?,
    val after: T?
)

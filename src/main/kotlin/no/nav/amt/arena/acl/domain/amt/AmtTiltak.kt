package no.nav.amt.arena.acl.domain.amt

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class AmtTiltak(
	@JsonProperty("id") val id: UUID,
	@JsonProperty("kode") val kode: String,
	@JsonProperty("navn") val navn: String
)

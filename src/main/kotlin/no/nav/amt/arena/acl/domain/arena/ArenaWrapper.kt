package no.nav.amt.arena.acl.domain.arena

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class ArenaOperation {
	I,
	U,
	D
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArenaWrapper(
	val table: String,

	@JsonProperty("op_type")
	val operation: ArenaOperation,

	@JsonProperty("op_ts")
	private val operationTimestampString: String,

	@JsonProperty("pos")
	val operationPosition: String,

	val before: String?,
	val after: String?
) {
	private val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

	val arenaId = when (table) {
		"SIAMO.TILTAK" -> getPayload(ArenaTiltak::class.java).TILTAKSKODE
		"SIAMO.TILTAKGJENNOMFORING" -> getPayload(ArenaTiltakGjennomforing::class.java).TILTAKGJENNOMFORING_ID.toString()
		"SIAMO.TILTAKDELTAKER" -> getPayload(ArenaTiltakDeltaker::class.java).TILTAKDELTAKER_ID.toString()
		else -> throw IllegalArgumentException("Table with name $table is not supported.")
	}

	val operationTimestamp = LocalDateTime.parse(operationTimestampString, opTsFormatter)

	private fun <T> getPayload(clazz: Class<T>): T {
		val objectMapper = jacksonObjectMapper()
		val data = before ?: after ?: throw NoSuchElementException("Both before and after is null")
		return objectMapper.readValue(data, clazz)
	}

}

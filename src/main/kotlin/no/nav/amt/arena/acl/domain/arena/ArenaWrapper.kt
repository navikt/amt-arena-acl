package no.nav.amt.arena.acl.domain.arena

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
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

	val before: JsonNode?,
	val after: JsonNode?
) {
	private val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

	val arenaId = when (table) {
		"SIAMO.TILTAK" -> getPayload(ArenaTiltak::class.java).TILTAKSKODE
		"SIAMO.TILTAKGJENNOMFORING" -> getPayload(ArenaTiltakGjennomforing::class.java).TILTAKGJENNOMFORING_ID.toString()
		"SIAMO.TILTAKDELTAKER" -> getPayload(ArenaTiltakDeltaker::class.java).TILTAKDELTAKER_ID.toString()
		else -> throw IllegalArgumentException("Table with name $table is not supported.")
	}

	val operationTimestamp = LocalDateTime.parse(operationTimestampString, opTsFormatter)

	fun toArenaData(): ArenaData {
		return ArenaData(
			arenaTableName = this.table,
			arenaId = this.arenaId,
			operation = this.operation.toAmtOperation(),
			operationPosition = this.operationPosition,
			operationTimestamp = this.operationTimestamp,
			before = this.before,
			after = this.after
		)

	}

	private fun ArenaOperation.toAmtOperation(): AmtOperation {
		return when (this) {
			ArenaOperation.I -> AmtOperation.CREATED
			ArenaOperation.U -> AmtOperation.MODIFIED
			ArenaOperation.D -> AmtOperation.DELETED
		}
	}

	private fun <T> getPayload(clazz: Class<T>): T {
		val objectMapper = ObjectMapperFactory.get()

		val data = when (operation) {
			ArenaOperation.I -> after
			ArenaOperation.U -> after
			ArenaOperation.D -> before
		} ?: throw NoSuchElementException("Both before and after is null")

		return objectMapper.treeToValue(data, clazz)
	}

}

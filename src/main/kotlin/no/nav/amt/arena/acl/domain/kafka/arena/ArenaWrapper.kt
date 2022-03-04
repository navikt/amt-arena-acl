package no.nav.amt.arena.acl.domain.kafka.arena

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_TILTAK_TABLE_NAME
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
	val operationTimestampString: String,

	@JsonProperty("pos")
	val operationPosition: String,

	val before: JsonNode?,
	val after: JsonNode?
) {
	private val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

	val arenaId = when (table) {
		ARENA_TILTAK_TABLE_NAME -> getPayload(ArenaTiltak::class.java).TILTAKSKODE
		ARENA_GJENNOMFORING_TABLE_NAME -> getPayload(ArenaGjennomforing::class.java).TILTAKGJENNOMFORING_ID.toString()
		ARENA_DELTAKER_TABLE_NAME -> getPayload(ArenaDeltaker::class.java).TILTAKDELTAKER_ID.toString()
		else -> throw IllegalArgumentException("Table with name $table is not supported.")
	}

	val operationTimestamp = LocalDateTime.parse(operationTimestampString, opTsFormatter)

	fun toArenaData(): ArenaDataDbo {
		return ArenaDataDbo(
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArenaKafkaMessageDto(
	val table: String,

	@JsonProperty("op_type")
	val opType: String,

	@JsonProperty("op_ts")
	val opTs: String,

	@JsonProperty("current_ts")
	val currentTs: String,

	val pos: String,
	val before: JsonNode?,
	val after: JsonNode?
)

data class ArenaKafkaMessage<D>(
	val arenaTableName: String,
	val operationType: AmtOperation,
	val operationTimestamp: LocalDateTime,
	val operationPosition: String,
	val before: D?,
	val after: D?
) {
	fun getData(): D {
		return when (operationType) {
			AmtOperation.CREATED -> after
			AmtOperation.MODIFIED -> after
			AmtOperation.DELETED -> before
		} ?: throw NoSuchElementException("Both before and after is null")
	}
}

typealias ArenaTiltakKafkaMessage = ArenaKafkaMessage<ArenaTiltak>

typealias ArenaGjennomforingKafkaMessage = ArenaKafkaMessage<ArenaGjennomforing>

typealias ArenaDeltakerKafkaMessage = ArenaKafkaMessage<ArenaDeltaker>


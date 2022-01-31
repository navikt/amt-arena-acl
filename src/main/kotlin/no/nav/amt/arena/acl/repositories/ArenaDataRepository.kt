package no.nav.amt.arena.acl.repositories

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.dto.LogStatusCountDto
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
open class ArenaDataRepository(
	private val template: NamedParameterJdbcTemplate,
) {

	private val mapper = jacksonObjectMapper()

	private val rowMapper = RowMapper { rs, _ ->
		val before = if (rs.getBytes("before") != null)
			mapper.readTree(rs.getBytes("before"))
		else null

		val after = if (rs.getBytes("after") != null)
			mapper.readTree(rs.getBytes("after"))
		else null

		ArenaData(
			id = rs.getInt("id"),
			arenaTableName = rs.getString("arena_table_name"),
			arenaId = rs.getString("arena_id"),
			operation = AmtOperation.valueOf(rs.getString("operation_type")),
			operationPosition = rs.getString("operation_pos"),
			operationTimestamp = rs.getTimestamp("operation_timestamp").toLocalDateTime(),
			ingestStatus = IngestStatus.valueOf(rs.getString("ingest_status")),
			ingestedTimestamp = rs.getTimestamp("ingested_timestamp")?.toLocalDateTime(),
			ingestAttempts = rs.getInt("ingest_attempts"),
			lastAttempted = rs.getTimestamp("last_attempted")?.toLocalDateTime(),
			before = before,
			after = after,
			note = rs.getString("note")
		)
	}

	fun upsert(arenaData: ArenaData) {
		val sql = """
			INSERT INTO arena_data(arena_table_name, arena_id, operation_type, operation_pos, operation_timestamp, ingest_status,
								   ingested_timestamp, ingest_attempts, last_attempted, before, after, note)
			VALUES (:arena_table_name,
					:arena_id,
					:operation_type,
					:operation_pos,
					:operation_timestamp,
					:ingest_status,
					:ingested_timestamp,
					:ingest_attempts,
					:last_attempted,
					:before::json,
					:after::json,
					:note)
			ON CONFLICT (arena_table_name, operation_type, operation_pos) DO UPDATE SET
					ingest_status      = :ingest_status,
					ingested_timestamp = :ingested_timestamp,
					ingest_attempts    = :ingest_attempts,
					last_attempted     = :last_attempted,
					note 			   = :note
		""".trimIndent()

		template.update(sql, arenaData.asParameterSource())
	}

	fun get(tableName: String, operation: AmtOperation, position: String): ArenaData {
		val sql = """
			SELECT *
			FROM arena_data
			WHERE arena_table_name = :arena_table_name
				AND operation_type = :operation_type
				AND operation_pos = :operation_pos
		""".trimIndent()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"arena_table_name" to tableName,
				"operation_type" to operation.name,
				"operation_pos" to position,
			)
		)

		return template.query(sql, parameters, rowMapper).firstOrNull()
			?: throw NoSuchElementException("Element from table $tableName, operation: $operation, position: $position does not exist")
	}

	fun getByIngestStatusIn(
		tableName: String,
		status: IngestStatus,
		offset: Int = 0,
		limit: Int = 500
	): List<ArenaData> {
		return getByIngestStatusIn(tableName, listOf(status), offset, limit)
	}

	fun getByIngestStatusIn(
		tableName: String,
		statuses: List<IngestStatus>,
		offset: Int = 0,
		limit: Int = 1000
	): List<ArenaData> {
		val sql = """
			SELECT *
			FROM arena_data
			WHERE ingest_status IN (:ingestStatuses)
			AND arena_table_name = :tableName
			ORDER BY operation_pos ASC
			OFFSET :offset LIMIT :limit
		""".trimIndent()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"ingestStatuses" to statuses.map { it.name }.toSet(),
				"tableName" to tableName,
				"offset" to offset,
				"limit" to limit
			)
		)

		return template.query(
			sql,
			parameters,
			rowMapper
		)
	}

	fun getStatusCount(): List<LogStatusCountDto> {
		val sql = """
			SELECT arena_table_name, ingest_status, count(*)
			FROM arena_data
			GROUP BY arena_table_name, ingest_status
		""".trimIndent()

		val logRowMapper = RowMapper { rs, _ ->
			LogStatusCountDto(
				table = rs.getString("arena_table_name"),
				status = IngestStatus.valueOf(rs.getString("ingest_status")),
				count = rs.getInt("count")
			)
		}

		return template.query(sql, logRowMapper)
	}

	fun getAll(): List<ArenaData> {
		val sql = """
			SELECT *
			FROM arena_data
		""".trimIndent()

		return template.query(
			sql,
			rowMapper
		)
	}

	fun deleteAllIgnoredData(): Int {
		val sql = """
			DELETE FROM arena_data WHERE ingest_status = 'IGNORED'
		""".trimIndent()

		return template.update(sql, emptyMap<String, Any>())
	}

}

private fun ArenaData.asParameterSource() = MapSqlParameterSource().addValues(
	mapOf(
		"arena_table_name" to arenaTableName,
		"arena_id" to arenaId,
		"operation_type" to operation.name,
		"operation_pos" to operationPosition,
		"operation_timestamp" to operationTimestamp,
		"ingest_status" to ingestStatus.name,
		"ingested_timestamp" to ingestedTimestamp,
		"ingest_attempts" to ingestAttempts,
		"last_attempted" to lastAttempted,
		"before" to if (before != null) jacksonObjectMapper().writeValueAsString(before) else null,
		"after" to if (after != null) jacksonObjectMapper().writeValueAsString(after) else null,
		"note" to note
	)
)

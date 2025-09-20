package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.domain.db.ArenaDataDbo
import no.nav.amt.arena.acl.domain.db.ArenaDataUpsertInput
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ArenaDataRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	fun upsert(upsertData: ArenaDataUpsertInput) {
		val sql = """
			INSERT INTO arena_data(
				arena_table_name,
				arena_id,
				operation_type,
				operation_pos,
				operation_timestamp,
				ingest_status,
				ingested_timestamp,
				before,
				after, note)
			VALUES (:arena_table_name,
				:arena_id,
				:operation_type,
				:operation_pos,
				:operation_timestamp,
				:ingest_status,
				:ingested_timestamp,
				:before::json,
				:after::json,
				:note)
			ON CONFLICT (arena_table_name, operation_type, operation_pos) DO UPDATE SET
				ingest_status      = :ingest_status,
				ingested_timestamp = :ingested_timestamp,
				note 			   = :note
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"arena_table_name" to upsertData.arenaTableName,
				"arena_id" to upsertData.arenaId,
				"operation_type" to upsertData.operation.name,
				"operation_pos" to upsertData.operationPosition,
				"operation_timestamp" to upsertData.operationTimestamp,
				"ingest_status" to upsertData.ingestStatus.name,
				"ingested_timestamp" to upsertData.ingestedTimestamp,
				"before" to upsertData.before,
				"after" to upsertData.after,
				"note" to upsertData.note,
			)
		)
	}

	fun updateIngestStatus(id: Int, ingestStatus: IngestStatus) {
		template.update(
			"UPDATE arena_data SET ingest_status = :ingest_status WHERE id = :id",
			sqlParameters(
				"ingest_status" to ingestStatus.name,
				"id" to id
			)
		)
	}

	fun updateIngestAttempts(id: Int, ingestAttempts: Int, note: String?) {
		val sql = """
			UPDATE arena_data SET
				ingest_attempts = :ingest_attempts,
			 	last_attempted = :last_attempted,
			 	note = :note
			  WHERE id = :id
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"ingest_attempts" to ingestAttempts,
				"last_attempted" to LocalDateTime.now(),
				"note" to note,
				"id" to id
			)
		)
	}

	fun get(tableName: String, operation: AmtOperation, position: String): ArenaDataDbo? {
		val sql = """
			SELECT *
			FROM arena_data
			WHERE
				arena_table_name = :arena_table_name
				AND operation_type = :operation_type
				AND operation_pos = :operation_pos
		""".trimIndent()

		return template.query(
			sql,
			sqlParameters(
				"arena_table_name" to tableName,
				"operation_type" to operation.name,
				"operation_pos" to position,
			),
			rowMapper
		).firstOrNull()
	}

	fun get(tableName: String, arenaId: String): List<ArenaDataDbo> {
		val sql = """
			SELECT *
			FROM arena_data
			WHERE arena_table_name = :arena_table_name
			AND arena_id = :arena_id
			ORDER BY id
		""".trimIndent()

		return template.query(
			sql,
			sqlParameters(
				"arena_table_name" to tableName,
				"arena_id" to arenaId,
			),
			rowMapper
		)
	}

	fun getByIngestStatus(
		tableName: String,
		status: IngestStatus,
		fromId: Int,
		limit: Int = 500
	): List<ArenaDataDbo> {
		val sql = """
			SELECT *
			FROM arena_data
			WHERE ingest_status = :ingestStatus
			AND arena_table_name = :tableName
			AND id >= :fromId
			ORDER BY id
			LIMIT :limit
		""".trimIndent()

		val parameters = sqlParameters(
			"ingestStatus" to status.name,
			"tableName" to tableName,
			"fromId" to fromId,
			"limit" to limit
		)

		return template.query(sql, parameters, rowMapper)
	}

	fun retryDeltakereMedGjennomforingIdOgStatus(arenaGjennomforingId: String, statuses: List<IngestStatus>): Int {
		val sql = """
			UPDATE arena_data
			SET
				ingest_status = '${IngestStatus.RETRY.name}',
				ingest_attempts = 0
			WHERE
				(
					after ->> 'TILTAKGJENNOMFORING_ID' = :gjennomforing_id
					OR before ->> 'TILTAKGJENNOMFORING_ID' = :gjennomforing_id
				)
				AND ingest_status in (:statuses)
				AND arena_table_name = '$ARENA_DELTAKER_TABLE_NAME'
			""".trimIndent()

		return template.update(
			sql,
			sqlParameters(
				"gjennomforing_id" to arenaGjennomforingId,
				"statuses" to statuses.map { it.name }
			)
		)
	}

	fun getFailedIngestStatusCount(): Int {
		val sql = """
			SELECT COUNT(1)
			FROM arena_data
			WHERE ingest_status = :status
			""".trimIndent()

		return template.queryForObject(
			sql,
			mapOf("status" to IngestStatus.FAILED.name),
			Int::class.java
		) ?: 0
	}

	fun deleteAllIgnoredData(): Int = template.update(
		"DELETE FROM arena_data WHERE ingest_status = 'IGNORED'",
		EmptySqlParameterSource()
	)

	// benyttes kun i tester
	fun getAll(): List<ArenaDataDbo> =
		template.query("SELECT * FROM arena_data", rowMapper)

	companion object {
		private val rowMapper = RowMapper { rs, _ ->
			ArenaDataDbo(
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
				before = rs.getString("before"),
				after = rs.getString("after"),
				note = rs.getString("note")
			)
		}
	}
}

package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.utils.getUUID
import org.postgresql.util.PSQLException
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
open class ArenaDataIdTranslationRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		ArenaDataIdTranslation(
			amtId = rs.getUUID("amt_id"),
			arenaTableName = rs.getString("arena_table_name"),
			arenaId = rs.getString("arena_id"),
			ignored = rs.getBoolean("is_ignored")
		)
	}

	fun insert(entry: ArenaDataIdTranslation) {
		val sql = """
			INSERT INTO arena_data_id_translation(amt_id, arena_table_name, arena_id, is_ignored)
			VALUES (:amt_id,
					:arena_table_name,
					:arena_id,
					:is_ignored)
		""".trimIndent()

		try {
			template.update(sql, entry.asParameterSource())
		} catch (e: DuplicateKeyException) {
			throw IllegalStateException("Translation entry on table ${entry.arenaTableName} with id ${entry.arenaId} already exist.")
		}

	}

	fun get(table: String, arenaId: String): ArenaDataIdTranslation? {
		val sql = """
			SELECT *
				FROM arena_data_id_translation
				WHERE arena_table_name = :arena_table_name
				  AND arena_id = :arena_id
		""".trimIndent()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"arena_table_name" to table,
				"arena_id" to arenaId
			)
		)

		return template.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	private fun ArenaDataIdTranslation.asParameterSource() = MapSqlParameterSource().addValues(
		mapOf(
			"amt_id" to amtId,
			"arena_table_name" to arenaTableName,
			"arena_id" to arenaId,
			"is_ignored" to ignored
		)
	)

}


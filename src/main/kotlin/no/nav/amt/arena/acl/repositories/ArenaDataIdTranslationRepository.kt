package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import no.nav.amt.arena.acl.utils.getUUID
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
open class ArenaDataIdTranslationRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		ArenaDataIdTranslationDbo(
			amtId = rs.getUUID("amt_id"),
			arenaTableName = rs.getString("arena_table_name"),
			arenaId = rs.getString("arena_id"),
		)
	}

	fun insert(entry: ArenaDataIdTranslationDbo) {
		val sql = """
			INSERT INTO arena_data_id_translation(amt_id, arena_table_name, arena_id)
			VALUES (:amt_id,
					:arena_table_name,
					:arena_id)
			ON CONFLICT (arena_table_name, arena_id) DO NOTHING
		""".trimIndent()

		try {
			template.update(sql, entry.asParameterSource())
		} catch (e: DuplicateKeyException) {
			throw IllegalStateException("Translation entry on table ${entry.arenaTableName} with id ${entry.arenaId} already exist.")
		}
	}

	fun get(table: String, arenaId: String): ArenaDataIdTranslationDbo? {
		val sql = """
			SELECT *
				FROM arena_data_id_translation
				WHERE arena_table_name = :arena_table_name
				  AND arena_id = :arena_id
		""".trimIndent()

		val parameters = sqlParameters(
			"arena_table_name" to table,
			"arena_id" to arenaId
		)

		return template.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	fun get(id: UUID): ArenaDataIdTranslationDbo? {
		val sql = """
			SELECT * FROM arena_data_id_translation WHERE amt_id = :amt_id
		""".trimIndent()

		val parameters = sqlParameters(
			"amt_id" to id
		)

		return template.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	fun getArenaId(table: String, amtId: UUID): ArenaDataIdTranslationDbo? {
		val sql = """
			SELECT *
				FROM arena_data_id_translation
				WHERE arena_table_name = :arena_table_name
				  AND amt_id = :amt_id
		""".trimIndent()

		val parameters = sqlParameters(
			"arena_table_name" to table,
			"amt_id" to amtId
		)

		return template.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	private fun ArenaDataIdTranslationDbo.asParameterSource() = sqlParameters(
		"amt_id" to amtId,
		"arena_table_name" to arenaTableName,
		"arena_id" to arenaId,
	)

}


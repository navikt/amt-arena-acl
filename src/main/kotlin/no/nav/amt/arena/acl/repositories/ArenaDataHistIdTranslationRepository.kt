package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.domain.db.ArenaDataHistIdTranslationDbo
import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import no.nav.amt.arena.acl.utils.getUUID
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ArenaDataHistIdTranslationRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	private val rowMapper =
		RowMapper { rs, _ ->
			ArenaDataHistIdTranslationDbo(
				amtId = rs.getUUID("amt_id"),
				arenaHistId = rs.getString("arena_hist_id"),
				arenaId = rs.getString("arena_id"),
			)
		}

	fun insert(entry: ArenaDataHistIdTranslationDbo) {
		val sql =
			"""
			INSERT INTO arena_data_hist_id_translation(amt_id, arena_hist_id, arena_id)
			VALUES (:amt_id,
					:arena_hist_id,
					:arena_id)
			ON CONFLICT (amt_id) DO NOTHING
			""".trimIndent()

		try {
			template.update(sql, entry.asParameterSource())
		} catch (_: DuplicateKeyException) {
			throw IllegalStateException("Translation entry on amtId ${entry.amtId} with arenaHistId ${entry.arenaHistId} already exist.")
		}
	}

	fun get(id: UUID): ArenaDataHistIdTranslationDbo? {
		val sql =
			"""
			SELECT * FROM arena_data_hist_id_translation WHERE amt_id = :amt_id
			""".trimIndent()

		val parameters =
			sqlParameters(
				"amt_id" to id,
			)

		return template
			.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	fun get(arenaHistId: String): ArenaDataHistIdTranslationDbo? {
		val sql =
			"""
			SELECT * FROM arena_data_hist_id_translation WHERE arena_hist_id = :arena_hist_id
			""".trimIndent()

		val parameters =
			sqlParameters(
				"arena_hist_id" to arenaHistId,
			)

		return template
			.query(sql, parameters, rowMapper)
			.firstOrNull()
	}

	private fun ArenaDataHistIdTranslationDbo.asParameterSource() =
		sqlParameters(
			"amt_id" to amtId,
			"arena_hist_id" to arenaHistId,
			"arena_id" to arenaId,
		)
}

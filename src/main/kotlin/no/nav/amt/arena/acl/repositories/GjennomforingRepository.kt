package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import no.nav.amt.arena.acl.utils.getLocalDate
import no.nav.amt.arena.acl.utils.getNullableUUID
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GjennomforingRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	fun upsert(arenaId: String, tiltakKode: String, isValid: Boolean) {
		val sql = """
			INSERT INTO gjennomforing(arena_id, tiltak_kode, is_valid)
			VALUES (:arenaId, :tiltakKode, :isValid) ON
			CONFLICT(arena_id) DO UPDATE SET is_valid=:isValid
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"arenaId" to arenaId,
				"tiltakKode" to tiltakKode, "isValid" to isValid
			)
		)
	}

	fun updateGjennomforingId(arenaId: String, gjennomforingId: UUID) {
		template.update(
			"UPDATE gjennomforing SET id=:gjennomforingId WHERE arena_id=:arenaId",
			sqlParameters("arenaId" to arenaId, "gjennomforingId" to gjennomforingId)
		)
	}

	fun get(arenaId: String): GjennomforingDbo? = template.query(
		"SELECT * FROM gjennomforing WHERE arena_id = :arenaId",
		sqlParameters("arenaId" to arenaId),
		rowMapper
	).firstOrNull()

	companion object {
		private val rowMapper = RowMapper { rs, _ ->
			GjennomforingDbo(
				arenaId = rs.getString("arena_id"),
				tiltakKode = rs.getString("tiltak_kode"),
				isValid = rs.getBoolean("is_valid"),
				createdAt = rs.getLocalDate("created_at"),
				id = rs.getNullableUUID("id")
			)
		}
	}
}

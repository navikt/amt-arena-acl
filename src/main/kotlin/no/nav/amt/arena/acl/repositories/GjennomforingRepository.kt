package no.nav.amt.arena.acl.repositories
import no.nav.amt.arena.acl.domain.Gjennomforing
import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import no.nav.amt.arena.acl.utils.getLocalDate
import no.nav.amt.arena.acl.utils.getNullableUUID
import no.nav.amt.arena.acl.utils.toModel
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GjennomforingRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	private val rowMapper = RowMapper { rs, _ ->
		GjennomforingDbo(
			arenaId = rs.getString("arena_id"),
			tiltakKode = rs.getString("tiltak_kode"),
			isValid = rs.getBoolean("is_valid"),
			createdAt = rs.getLocalDate("created_at"),
			id = rs.getNullableUUID("id")
		)
	}
	fun upsert(arenaId: String, tiltakKode: String, isValid: Boolean) {
		val sql = """
			INSERT INTO gjennomforing(arena_id, tiltak_kode, is_valid)
			VALUES (:arenaId, :tiltakKode, :isValid) ON
			CONFLICT(arena_id) DO UPDATE SET is_valid=:isValid
		""".trimIndent()
		val parameters = sqlParameters("arenaId" to arenaId, "tiltakKode" to tiltakKode, "isValid" to isValid)
		template.update(sql, parameters)
	}

	fun updateGjennomforingId(arenaId: String, gjennomforingId: UUID) {
		template.update(
			"UPDATE gjennomforing SET id=:gjennomforingId WHERE arena_id=:arenaId",
			sqlParameters("arenaId" to arenaId, "gjennomforingId" to gjennomforingId))
	}

	fun get(arenaId: String): Gjennomforing? {
		val sql = """
			SELECT * FROM gjennomforing WHERE arena_id = :arenaId
		""".trimIndent()
		val parameters = sqlParameters("arenaId" to arenaId)

		return template.query(sql, parameters, rowMapper).firstOrNull()?.toModel()
	}
}

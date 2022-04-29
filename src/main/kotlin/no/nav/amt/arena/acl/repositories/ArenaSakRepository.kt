package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.domain.db.ArenaSakDbo
import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import no.nav.amt.arena.acl.utils.getZonedDateTime
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
open class ArenaSakRepository(
	private val template: NamedParameterJdbcTemplate,
) {

	private val rowMapper = RowMapper { rs, _ ->
		ArenaSakDbo(
			id = rs.getInt("id"),
			arenaSakId = rs.getLong("arena_sak_id"),
			aar = rs.getInt("aar"),
			lopenr = rs.getInt("lopenr"),
			ansvarligEnhetId = rs.getString("ansvarlig_enhet_id"),
			createdAt = rs.getZonedDateTime("created_at")
		)
	}

	fun hentSakMedArenaId(arenaSakId: Long): ArenaSakDbo? {
		val sql = """
			select * from arena_sak where arena_sak_id = :arenaSakId
		""".trimIndent()

		val parameters = sqlParameters("arenaSakId" to arenaSakId)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun upsertSak(
		arenaSakId: Long,
		aar: Int,
		lopenr: Int,
		ansvarligEnhetId: String,
	) {
		val sql = """
			insert into arena_sak(arena_sak_id, aar, lopenr, ansvarlig_enhet_id, created_at)
				values (:arenaSakId, :aar, :lopenr, :ansvarligEnhetId, current_timestamp)
				ON CONFLICT (arena_sak_id) DO UPDATE
				SET ansvarlig_enhet_id = :ansvarligEnhetId
		""".trimIndent()

		val parameters = sqlParameters(
			"arenaSakId" to arenaSakId,
			"aar" to aar,
			"lopenr" to lopenr,
			"ansvarligEnhetId" to ansvarligEnhetId,
		)

		template.update(sql, parameters)
	}

}

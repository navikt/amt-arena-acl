package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class GjennomforingRepository(
	private val template: NamedParameterJdbcTemplate,
) {

	fun upsert(arenaId: String, tiltakKode: String, isValid: Boolean) {
		template.update(
			"INSERT INTO gjennomforing(arena_id, tiltak_kode, is_valid) " +
				"VALUES (:arenaId, :tiltakKode, :isValid) ON " +
				"CONFLICT(arena_id) DO UPDATE SET is_valid=:isValid",
			sqlParameters("arenaId" to arenaId, "tiltakKode" to tiltakKode, "isValid" to isValid)
		)
	}

	fun isValid(arenaId: String): Boolean {
		return template.query(
			"SELECT is_valid FROM gjennomforing WHERE arena_id = :arenaId",
			sqlParameters("arenaId" to arenaId)
		) { rs, _ -> rs.getBoolean("is_valid") }.first()
	}

}

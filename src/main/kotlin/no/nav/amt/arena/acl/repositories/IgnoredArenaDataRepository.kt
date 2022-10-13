package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import no.nav.amt.arena.acl.utils.getUUID
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class IgnoredArenaDataRepository(
	private val template: NamedParameterJdbcTemplate,
) {

	fun ignore(id: UUID) {
		template.update(
			"INSERT INTO ignored_arena_data(id) VALUES (:id) ON CONFLICT DO NOTHING",
			sqlParameters("id" to id)
		)
	}

	fun isIgnored(id: UUID): Boolean {
		return template.query(
			"SELECT id FROM ignored_arena_data WHERE id = :id",
			sqlParameters("id" to id)
		) { rs, _ -> rs.getUUID("id") }.isNotEmpty()
	}

}

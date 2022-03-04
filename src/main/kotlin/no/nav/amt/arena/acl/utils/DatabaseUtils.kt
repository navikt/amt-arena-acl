package no.nav.amt.arena.acl.utils

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource

object DatabaseUtils {

	fun <V> sqlParameters(vararg pairs: Pair<String, V>): MapSqlParameterSource {
		return MapSqlParameterSource().addValues(pairs.toMap())
	}

}

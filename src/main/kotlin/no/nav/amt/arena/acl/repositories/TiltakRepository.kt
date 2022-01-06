package no.nav.amt.arena.acl.repositories

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import no.nav.amt.arena.acl.domain.amt.AmtTiltak
import no.nav.amt.arena.acl.utils.getUUID
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.TimeUnit

@Component
open class TiltakRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val cache: Cache<String, AmtTiltak> = Caffeine.newBuilder()
		.maximumSize(200)
		.expireAfterWrite(10, TimeUnit.MINUTES)
		.recordStats()
		.build()

	private val rowMapper = RowMapper { rs, _ ->
		AmtTiltak(
			id = rs.getUUID("id"),
			kode = rs.getString("kode"),
			navn = rs.getString("navn")
		)
	}

	fun upsert(kode: String, navn: String): AmtTiltak {
		val sql = """
			INSERT INTO arena_tiltak(id, kode, navn)
			VALUES (:id,
					:kode,
					:navn)
			ON CONFLICT (kode) DO UPDATE SET navn = :navn
		""".trimIndent()

		val id = UUID.randomUUID()

		val parameters = MapSqlParameterSource().addValues(
			mapOf(
				"id" to id,
				"kode" to kode,
				"navn" to navn
			)
		)

		template.update(sql, parameters)

		val tiltak = getByKode(kode)
			?: throw NoSuchElementException("Tiltak med kode $kode kan ikke hentes fra databasen.")

		return tiltak
	}

	fun getByKode(kode: String): AmtTiltak? {
		val cachedTiltak = cache.getIfPresent(kode)

		if (cachedTiltak != null) {
			return cachedTiltak
		}

		val sql = """
			SELECT *
			FROM arena_tiltak
			WHERE kode = :kode
		""".trimIndent()

		val tiltak = template.query(sql, singletonParameterMap("kode", kode), rowMapper).firstOrNull()

		if (tiltak != null) {
			cache.put(kode, tiltak)
		}

		return tiltak
	}

	fun getCache(): Cache<String, AmtTiltak> {
		return cache
	}


	private fun singletonParameterMap(key: String, value: Any): MapSqlParameterSource {
		return MapSqlParameterSource().addValues(
			mapOf(
				key to value
			)
		)
	}
}

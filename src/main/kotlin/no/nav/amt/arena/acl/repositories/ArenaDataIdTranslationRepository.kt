package no.nav.amt.arena.acl.repositories

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.utils.getUUID
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.TimeUnit

@Component
open class ArenaDataIdTranslationRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val cache: Cache<Pair<String, String>, ArenaDataIdTranslation> = Caffeine.newBuilder()
		.maximumSize(10000)
		.expireAfterWrite(10, TimeUnit.MINUTES)
		.build()

	private val containsCache: Cache<Pair<String, String>, Boolean> = Caffeine.newBuilder()
		.maximumSize(10000)
		.expireAfterWrite(10, TimeUnit.MINUTES)
		.build()

	private val rowMapper = RowMapper { rs, _ ->
		ArenaDataIdTranslation(
			amtId = rs.getUUID("amt_id"),
			arenaTableName = rs.getString("arena_table_name"),
			arenaId = rs.getString("arena_id"),
			ignored = rs.getBoolean("is_ignored"),
			currentHash = rs.getString("current_hash")
		)
	}

	fun insert(entry: ArenaDataIdTranslation) {
		val sql = """
			INSERT INTO arena_data_id_translation(amt_id, arena_table_name, arena_id, is_ignored, current_hash)
			VALUES (:amt_id,
					:arena_table_name,
					:arena_id,
					:is_ignored,
					:current_hash)
			ON CONFLICT (arena_table_name, arena_id) DO UPDATE SET
				is_ignored = :is_ignored,
				current_hash = :current_hash
		""".trimIndent()

		try {
			template.update(sql, entry.asParameterSource())
			cache.put(Pair(entry.arenaTableName, entry.arenaId), entry)
			containsCache.put(Pair(entry.arenaTableName, entry.arenaId), true)
		} catch (e: DuplicateKeyException) {
			throw IllegalStateException("Translation entry on table ${entry.arenaTableName} with id ${entry.arenaId} already exist.")
		}

	}

	fun getAmtId(table: String, arenaId: String): UUID? {
		return get(table, arenaId)?.amtId
	}

	fun get(table: String, arenaId: String): ArenaDataIdTranslation? {
		val key = Pair(table, arenaId)
		val contains = containsCache.getIfPresent(key)

		return if (contains != null) {
			return cache.getIfPresent(key)
		} else {
			getFromDatabase(table, arenaId)
		}
	}

	fun getAll(): List<ArenaDataIdTranslation> {
		val sql = """
			SELECT *
			FROM arena_data_id_translation
		""".trimIndent()

		return template.query(sql, rowMapper)
	}

	private fun getFromDatabase(table: String, arenaId: String): ArenaDataIdTranslation? {
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

		val entry = template.query(sql, parameters, rowMapper)
			.firstOrNull()

		val cacheKey = Pair(table, arenaId)

		if (entry != null) {
			cache.put(cacheKey, entry)
			containsCache.put(cacheKey, true)
		} else {
			containsCache.put(cacheKey, false)
		}

		return entry
	}

	private fun ArenaDataIdTranslation.asParameterSource() = MapSqlParameterSource().addValues(
		mapOf(
			"amt_id" to amtId,
			"arena_table_name" to arenaTableName,
			"arena_id" to arenaId,
			"is_ignored" to ignored,
			"current_hash" to currentHash
		)
	)

}


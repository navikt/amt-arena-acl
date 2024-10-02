package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import no.nav.amt.arena.acl.utils.getLocalDate
import no.nav.amt.arena.acl.utils.getLocalDateTime
import no.nav.amt.arena.acl.utils.getUUID
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DeltakerRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	private val rowMapper = RowMapper { rs, _ ->
		DeltakerDbo(
			id = rs.getUUID("id"),
			arenaId = rs.getLong("arena_id"),
			personId = rs.getLong("person_id"),
			gjennomforingId = rs.getLong("gjennomforing_id"),
			datoFra = rs.getLocalDate("dato_fra"),
			datoTil = rs.getLocalDate("dato_til"),
			regDato = rs.getLocalDateTime("reg_dato"),
			modDato = rs.getLocalDateTime("mod_dato"),
			status = rs.getString("status"),
			datoStatusEndring = rs.getLocalDateTime("dato_statusendring"),
			eksternId = rs.getUUID("ekstern_id"),
			arenaSourceTable = rs.getString("arena_source_table"),
			createdAt = rs.getLocalDateTime("created_at"),
			modifiedAt = rs.getLocalDateTime("modified_at")
		)
	}

	fun upsert(deltakerDbo: DeltakerInsertDbo) {
		val sql = """
			INSERT INTO deltaker(
				id,
				arena_id,
				person_id,
				gjennomforing_id,
				dato_fra,
				dato_til,
				reg_dato,
				mod_dato,
				status,
				dato_statusendring,
				ekstern_id,
				arena_source_table)
			VALUES (
				:id,
				:arenaId,
				:personId,
				:gjennomforingId,
				:datoFra,
				:datoTil,
				:regDato,
				:modDato,
				:status,
				:datoStatusEndring,
				:eksternId,
				:arenaSourceTable
			) ON CONFLICT(arena_id, arena_source_table) DO UPDATE SET
				arena_id = :arenaId,
				person_id = :personId,
				gjennomforing_id = :gjennomforingId,
				dato_fra = :datoFra,
				dato_til = :datoTil,
				reg_dato = :regDato,
				mod_dato = :modDato,
				status = :status,
				dato_statusendring = :datoStatusEndring,
				ekstern_id = :eksternId,
				arena_source_table = :arenaSourceTable,
				modified_at = CURRENT_TIMESTAMP
		""".trimIndent()
		val parameters = sqlParameters(
			"id" to UUID.randomUUID(),
			"arenaId" to deltakerDbo.arenaId,
			"personId" to deltakerDbo.personId,
			"gjennomforingId" to deltakerDbo.gjennomforingId,
			"datoFra" to deltakerDbo.datoFra,
			"datoTil" to deltakerDbo.datoTil,
			"regDato" to deltakerDbo.regDato,
			"modDato" to deltakerDbo.modDato,
			"status" to deltakerDbo.status,
			"datoStatusEndring" to deltakerDbo.datoStatusEndring,
			"eksternId" to deltakerDbo.eksternId,
			"arenaSourceTable" to deltakerDbo.arenaSourceTable,
		)
		template.update(sql, parameters)
	}

	fun get(arenaId: Long, arenaTable: String): DeltakerDbo? {
		val sql = "SELECT * FROM deltaker WHERE arena_id = :arenaId AND arena_source_table = :arenaTable"
		val parameters = sqlParameters(
			"arenaId" to arenaId,
			"arenaTable" to arenaTable,
		)
		return template.query(sql, parameters, rowMapper).firstOrNull()
	}



}

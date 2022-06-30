package no.nav.amt.arena.acl.repositories

import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.utils.*
import no.nav.amt.arena.acl.utils.DatabaseUtils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class ArenaGjennomforingRepository (
	val template: NamedParameterJdbcTemplate
) {
	val rowMapper = RowMapper { rs, _ ->
		ArenaGjennomforingDbo(
			id = rs.getUUID("id"),
			arenaSakId = rs.getLong("arena_sak_id"),
			tiltakKode = rs.getString("tiltak_kode"),
			virksomhetsnummer = rs.getString("virksomhetsnummer"),
			navn = rs.getString("navn"),
			startDato = rs.getNullableLocalDate("start_dato"),
			sluttDato = rs.getNullableLocalDate("slutt_dato"),
			registrertDato = rs.getLocalDateTime("registrert_dato"),
			fremmoteDato = rs.getNullableLocalDateTime("fremmote_dato"),
			status = AmtGjennomforing.Status.valueOf(rs.getString("status")),
			ansvarligNavEnhetId = rs.getString("ansvarlig_nav_enhetId"),
			opprettetAar = rs.getNullableInteger("opprettet_aar"),
			lopenr = rs.getNullableInteger("lopenr"),
		)
	}

	fun get(id: UUID) : ArenaGjennomforingDbo? {
		//language=PostgreSQL
		val sql = """
			SELECT * FROM arena_gjennomforing where id = :id
		""".trimIndent()
		val params = sqlParameters("id" to id)
		return template.query(sql, params, rowMapper).firstOrNull()
	}

	fun getBySakId(arenaSakId: Long) : ArenaGjennomforingDbo? {
		//language=PostgreSQL
		val sql = """
			SELECT * FROM arena_gjennomforing where arena_sak_id = :arena_sak_id
		""".trimIndent()
		val params = sqlParameters("arena_sak_id" to arenaSakId)
		return template.query(sql, params, rowMapper).firstOrNull()
	}

	fun upsert(gjennomforing: ArenaGjennomforingDbo) {
			//language=PostgreSQL
			val sql = """
				INSERT INTO arena_gjennomforing (id, tiltak_kode, virksomhetsnummer, navn, start_dato, slutt_dato, registrert_dato, fremmote_dato, status, ansvarlig_nav_enhetId, opprettet_aar, lopenr, arena_sak_id)
				VALUES (:id, :tiltak_kode, :virksomhetsnummer, :navn, :start_dato, :slutt_dato, :registrert_dato, :fremmote_dato, :status, :ansvarlig_nav_enhetId, :opprettet_aar, :lopenr, :arena_sak_id)
				ON CONFLICT(id) DO UPDATE SET
					tiltak_kode = :tiltak_kode,
					virksomhetsnummer = :virksomhetsnummer,
					navn = :navn,
					start_dato = :start_dato,
					slutt_dato = :slutt_dato,
					registrert_dato = :registrert_dato,
					fremmote_dato = :fremmote_dato,
					status = :status,
					ansvarlig_nav_enhetId = :ansvarlig_nav_enhetId,
					opprettet_aar = :opprettet_aar,
					lopenr = :lopenr,
					arena_sak_id = :arena_sak_id,
					modified_at = CURRENT_TIMESTAMP

			""".trimIndent()

		val params = sqlParameters(
				"id" to gjennomforing.id,
				"tiltak_kode" to gjennomforing.tiltakKode,
				"virksomhetsnummer" to gjennomforing.virksomhetsnummer,
				"navn" to gjennomforing.navn,
				"start_dato" to gjennomforing.startDato,
				"slutt_dato" to gjennomforing.sluttDato,
				"registrert_dato" to gjennomforing.registrertDato,
				"fremmote_dato" to gjennomforing.fremmoteDato,
				"status" to gjennomforing.status.toString(),
				"ansvarlig_nav_enhetId" to gjennomforing.ansvarligNavEnhetId,
				"opprettet_aar" to gjennomforing.opprettetAar,
				"lopenr" to gjennomforing.lopenr,
				"arena_sak_id" to gjennomforing.arenaSakId
			)

		template.update(sql, params)
	}
}

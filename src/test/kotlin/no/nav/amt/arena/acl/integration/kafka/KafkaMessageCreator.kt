package no.nav.amt.arena.acl.integration.kafka

import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforing
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonNode
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object KafkaMessageCreator {

	private const val GENERIC_STRING = "STRING_NOT_SET"
	private const val GENERIC_INT = Int.MIN_VALUE
	private const val GENERIC_LONG = Long.MIN_VALUE
	private const val GENERIC_FLOAT = Float.MIN_VALUE

	private val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
	private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

	private var pos = 0

	fun opprettArenaDeltaker(
		arenaDeltaker: ArenaDeltaker,
		opType: String = "I",
		opPos: String? = null
	): ArenaKafkaMessageDto {
		return arenaKafkaMessageDto(opType, arenaDeltaker, ARENA_DELTAKER_TABLE_NAME, opPos)
	}

	fun opprettArenaGjennomforing(
		arenaGjennomforing: ArenaGjennomforing,
		opType: String = "I",
	): ArenaKafkaMessageDto {
		return arenaKafkaMessageDto(opType, arenaGjennomforing, ARENA_GJENNOMFORING_TABLE_NAME)
	}

	private fun <T> arenaKafkaMessageDto(
		opType: String,
		arenaData: T,
		arenaTableName: String,
		opPos: String? = null
	): ArenaKafkaMessageDto {
		val before = when (opType) {
			"I" -> null
			"U" -> arenaData
			"D" -> arenaData
			else -> throw IllegalArgumentException("Ugyldig op_type $opType")
		}

		val after = when (opType) {
			"I" -> arenaData
			"U" -> arenaData
			"D" -> null
			else -> throw IllegalArgumentException("Ugyldig op_type $opType")
		}

		return ArenaKafkaMessageDto(
			table = arenaTableName,
			opType = opType,
			opTs = opTsFormatter.format(LocalDateTime.now()),
			pos = opPos ?: (pos++).toString(),
			before = before?.let { toJsonNode(toJsonString(it)) },
			after = after?.let { toJsonNode(toJsonString(it)) },
		)
	}

	fun baseGjennomforing(
		arenaGjennomforingId: Long,
		tiltakskode: String,
		navn: String,
		tiltakstatuskode: String,
		arbgivIdArrangor: Long? = null,
		datoFra: LocalDateTime? = null,
		datoTil: LocalDateTime? = null
	): ArenaGjennomforing {
		return ArenaGjennomforing(
			TILTAKGJENNOMFORING_ID = arenaGjennomforingId,
			SAK_ID = 0,
			TILTAKSKODE = tiltakskode,
			ANTALL_DELTAKERE = GENERIC_INT,
			ANTALL_VARIGHET = GENERIC_INT,
			DATO_FRA = datoFra?.let { dateFormatter.format(it) },
			DATO_TIL = datoTil?.let { dateFormatter.format(it) },
			FAGPLANKODE = GENERIC_STRING,
			MAALEENHET_VARIGHET = GENERIC_STRING,
			TEKST_FAGBESKRIVELSE = GENERIC_STRING,
			TEKST_KURSSTED = GENERIC_STRING,
			TEKST_MAALGRUPPE = GENERIC_STRING,
			STATUS_TREVERDIKODE_INNSOKNING = GENERIC_STRING,
			REG_DATO = GENERIC_STRING,
			REG_USER = GENERIC_STRING,
			MOD_DATO = GENERIC_STRING,
			MOD_USER = GENERIC_STRING,
			LOKALTNAVN = navn,
			TILTAKSTATUSKODE = tiltakstatuskode,
			PROSENT_DELTID = GENERIC_FLOAT,
			KOMMENTAR = GENERIC_STRING,
			ARBGIV_ID_ARRANGOR = arbgivIdArrangor,
			PROFILELEMENT_ID_GEOGRAFI = GENERIC_STRING,
			KLOKKETID_FREMMOTE = null,
			DATO_FREMMOTE = GENERIC_STRING,
			BEGRUNNELSE_STATUS = GENERIC_STRING,
			AVTALE_ID = GENERIC_LONG,
			AKTIVITET_ID = GENERIC_LONG,
			DATO_INNSOKNINGSTART = GENERIC_STRING,
			GML_FRA_DATO = GENERIC_STRING,
			GML_TIL_DATO = GENERIC_STRING,
			AETAT_FREMMOTEREG = GENERIC_STRING,
			AETAT_KONTERINGSSTED = GENERIC_STRING,
			OPPLAERINGNIVAAKODE = GENERIC_STRING,
			TILTAKGJENNOMFORING_ID_REL = GENERIC_STRING,
			VURDERING_GJENNOMFORING = GENERIC_STRING,
			PROFILELEMENT_ID_OPPL_TILTAK = GENERIC_STRING,
			DATO_OPPFOLGING_OK = GENERIC_STRING,
			PARTISJON = GENERIC_LONG,
			MAALFORM_KRAVBREV = GENERIC_STRING
		)
	}

	fun baseDeltaker(
		arenaDeltakerId: Long,
		personId: Long,
		tiltakGjennomforingId: Long,
		deltakerStatusKode: String = "GJENN",
		statusAarsak: String? = null,
		startDato: LocalDate? = null,
		sluttDato: LocalDate? = null,
		datoStatusEndring: LocalDateTime? = null,
		registrertDato: LocalDateTime = LocalDateTime.now(),
	): ArenaDeltaker {
		return ArenaDeltaker(
			TILTAKDELTAKER_ID = arenaDeltakerId,
			PERSON_ID = personId,
			TILTAKGJENNOMFORING_ID = tiltakGjennomforingId,
			DELTAKERSTATUSKODE = deltakerStatusKode,
			DELTAKERTYPEKODE = GENERIC_STRING,
			AARSAKVERDIKODE_STATUS = statusAarsak,
			OPPMOTETYPEKODE = GENERIC_STRING,
			PRIORITET = GENERIC_INT,
			BEGRUNNELSE_INNSOKT = GENERIC_STRING,
			BEGRUNNELSE_PRIORITERING = GENERIC_STRING,
			REG_DATO = dateFormatter.format(registrertDato),
			REG_USER = GENERIC_STRING,
			MOD_DATO = GENERIC_STRING,
			MOD_USER = GENERIC_STRING,
			DATO_SVARFRIST = GENERIC_STRING,
			DATO_FRA = startDato?.let { dateFormatter.format(it.atStartOfDay()) },
			DATO_TIL = sluttDato?.let { dateFormatter.format(it.atStartOfDay()) },
			BEGRUNNELSE_STATUS = GENERIC_STRING,
			PROSENT_DELTID = GENERIC_FLOAT,
			BRUKERID_STATUSENDRING = GENERIC_STRING,
			DATO_STATUSENDRING = datoStatusEndring?.let { dateFormatter.format(it) },
			AKTIVITET_ID = GENERIC_LONG,
			BRUKERID_ENDRING_PRIORITERING = GENERIC_STRING,
			DATO_ENDRING_PRIORITERING = GENERIC_STRING,
			DOKUMENTKODE_SISTE_BREV = GENERIC_STRING,
			STATUS_INNSOK_PAKKE = GENERIC_STRING,
			STATUS_OPPTAK_PAKKE = GENERIC_STRING,
			OPPLYSNINGER_INNSOK = GENERIC_STRING,
			PARTISJON = GENERIC_INT,
			BEGRUNNELSE_BESTILLING = GENERIC_STRING,
			ANTALL_DAGER_PR_UKE = GENERIC_INT,
		)
	}

}

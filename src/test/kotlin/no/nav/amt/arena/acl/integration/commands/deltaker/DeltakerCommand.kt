package no.nav.amt.arena.acl.integration.commands.deltaker

import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.integration.commands.Command

abstract class DeltakerCommand : Command() {

	abstract fun execute(
		position: String,
		executor: (wrapper: ArenaKafkaMessageDto) -> DeltakerResult
	): DeltakerResult

	fun createPayload(input: DeltakerInput): JsonNode {
		val data = ArenaDeltaker(
			TILTAKDELTAKER_ID = input.tiltakDeltakerId,
			PERSON_ID = input.personId,
			TILTAKGJENNOMFORING_ID = input.tiltakgjennomforingId,
			DELTAKERSTATUSKODE = input.deltakerStatusKode,
			DELTAKERTYPEKODE = GENERIC_STRING,
			AARSAKVERDIKODE_STATUS = GENERIC_STRING,
			OPPMOTETYPEKODE = GENERIC_STRING,
			PRIORITET = GENERIC_INT,
			BEGRUNNELSE_INNSOKT = GENERIC_STRING,
			BEGRUNNELSE_PRIORITERING = GENERIC_STRING,
			REG_DATO = dateFormatter.format(input.registrertDato),
			REG_USER = GENERIC_STRING,
			MOD_DATO = GENERIC_STRING,
			MOD_USER = GENERIC_STRING,
			DATO_SVARFRIST = GENERIC_STRING,
			DATO_FRA = dateFormatter.format(input.datoFra.atStartOfDay()),
			DATO_TIL = dateFormatter.format(input.datoTil.atStartOfDay()),
			BEGRUNNELSE_STATUS = GENERIC_STRING,
			PROSENT_DELTID = input.prosentDeltid,
			BRUKERID_STATUSENDRING = GENERIC_STRING,
			DATO_STATUSENDRING = dateFormatter.format(input.datoStatusEndring.atStartOfDay()),
			AKTIVITET_ID = GENERIC_LONG,
			BRUKERID_ENDRING_PRIORITERING = GENERIC_STRING,
			DATO_ENDRING_PRIORITERING = GENERIC_STRING,
			DOKUMENTKODE_SISTE_BREV = GENERIC_STRING,
			STATUS_INNSOK_PAKKE = GENERIC_STRING,
			STATUS_OPPTAK_PAKKE = GENERIC_STRING,
			OPPLYSNINGER_INNSOK = GENERIC_STRING,
			PARTISJON = GENERIC_INT,
			BEGRUNNELSE_BESTILLING = input.innsokBegrunnelse,
			ANTALL_DAGER_PR_UKE = input.antallDagerPerUke
		)
		return objectMapper.readTree(objectMapper.writeValueAsString(data))
	}

}

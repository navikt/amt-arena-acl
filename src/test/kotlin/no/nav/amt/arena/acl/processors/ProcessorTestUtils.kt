package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltakerKafkaMessage
import no.nav.amt.arena.acl.integration.commands.Command
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerInput
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import java.time.LocalDateTime

fun createArenaDeltakerKafkaMessage(
	input: DeltakerInput
): ArenaDeltakerKafkaMessage {

	val payload = createDeltakerPayload(input)

	return ArenaDeltakerKafkaMessage(
		arenaTableName = ARENA_DELTAKER_TABLE_NAME,
		operationType = AmtOperation.CREATED,
		operationTimestamp = LocalDateTime.now(),
		operationPosition = "3",
		after = payload,
		before = null
	)
}

fun createDeltakerPayload(input: DeltakerInput): ArenaDeltaker {
	return ArenaDeltaker(
		TILTAKDELTAKER_ID = input.tiltakDeltakerId,
		PERSON_ID = input.personId,
		TILTAKGJENNOMFORING_ID = input.tiltakgjennomforingId,
		DELTAKERSTATUSKODE = input.deltakerStatusKode,
		DELTAKERTYPEKODE = Command.GENERIC_STRING,
		AARSAKVERDIKODE_STATUS = input.statusAarsak,
		OPPMOTETYPEKODE = Command.GENERIC_STRING,
		PRIORITET = Command.GENERIC_INT,
		BEGRUNNELSE_INNSOKT = Command.GENERIC_STRING,
		BEGRUNNELSE_PRIORITERING = Command.GENERIC_STRING,
		REG_DATO = Command.dateFormatter.format(input.registrertDato),
		REG_USER = Command.GENERIC_STRING,
		MOD_DATO = Command.GENERIC_STRING,
		MOD_USER = Command.GENERIC_STRING,
		DATO_SVARFRIST = Command.GENERIC_STRING,
		DATO_FRA = Command.dateFormatter.format(input.datoFra.atStartOfDay()),
		DATO_TIL = Command.dateFormatter.format(input.datoTil.atStartOfDay()),
		BEGRUNNELSE_STATUS = Command.GENERIC_STRING,
		PROSENT_DELTID = input.prosentDeltid,
		BRUKERID_STATUSENDRING = Command.GENERIC_STRING,
		DATO_STATUSENDRING = Command.dateFormatter.format(input.datoStatusEndring.atStartOfDay()),
		AKTIVITET_ID = Command.GENERIC_LONG,
		BRUKERID_ENDRING_PRIORITERING = Command.GENERIC_STRING,
		DATO_ENDRING_PRIORITERING = Command.GENERIC_STRING,
		DOKUMENTKODE_SISTE_BREV = Command.GENERIC_STRING,
		STATUS_INNSOK_PAKKE = Command.GENERIC_STRING,
		STATUS_OPPTAK_PAKKE = Command.GENERIC_STRING,
		OPPLYSNINGER_INNSOK = Command.GENERIC_STRING,
		PARTISJON = Command.GENERIC_INT,
		BEGRUNNELSE_BESTILLING = input.innsokBegrunnelse,
		ANTALL_DAGER_PR_UKE = input.antallDagerPerUke
	)
}

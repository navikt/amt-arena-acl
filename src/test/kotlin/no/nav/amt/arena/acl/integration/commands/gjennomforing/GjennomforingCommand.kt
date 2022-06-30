package no.nav.amt.arena.acl.integration.commands.gjennomforing

import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforing
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.integration.commands.Command

abstract class GjennomforingCommand : Command() {

	abstract fun execute(
		position: String,
		executor: (wrapper: ArenaKafkaMessageDto) -> GjennomforingResult
	): GjennomforingResult

	fun createPayload(input: GjennomforingInput): JsonNode {
		val data = ArenaGjennomforing(
			TILTAKGJENNOMFORING_ID = input.gjennomforingId,
			SAK_ID = input.sakId,
			TILTAKSKODE = input.tiltakKode,
			ANTALL_DELTAKERE = GENERIC_INT,
			ANTALL_VARIGHET = GENERIC_INT,
			DATO_FRA = dateFormatter.format(input.startDato.atStartOfDay()),
			DATO_TIL = dateFormatter.format(input.sluttDato.atStartOfDay()),
			FAGPLANKODE = GENERIC_STRING,
			MAALEENHET_VARIGHET = GENERIC_STRING,
			TEKST_FAGBESKRIVELSE = GENERIC_STRING,
			TEKST_KURSSTED = GENERIC_STRING,
			TEKST_MAALGRUPPE = GENERIC_STRING,
			STATUS_TREVERDIKODE_INNSOKNING = GENERIC_STRING,
			REG_DATO = dateFormatter.format(input.registrertDato),
			REG_USER = GENERIC_STRING,
			MOD_DATO = GENERIC_STRING,
			MOD_USER = GENERIC_STRING,
			LOKALTNAVN = input.navn,
			TILTAKSTATUSKODE = input.tiltakStatusKode,
			PROSENT_DELTID = GENERIC_FLOAT,
			KOMMENTAR = GENERIC_STRING,
			ARBGIV_ID_ARRANGOR = input.arbeidsgiverIdArrangor,
			PROFILELEMENT_ID_GEOGRAFI = GENERIC_STRING,
			KLOKKETID_FREMMOTE = null,
			DATO_FREMMOTE = dateFormatter.format(input.fremmoteDato),
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

		return objectMapper.readTree(objectMapper.writeValueAsString(data))
	}
}

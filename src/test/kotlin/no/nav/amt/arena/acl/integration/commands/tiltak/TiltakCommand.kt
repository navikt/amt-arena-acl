package no.nav.amt.arena.acl.integration.commands.tiltak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaTiltak
import no.nav.amt.arena.acl.integration.commands.Command

abstract class TiltakCommand : Command() {

	abstract fun execute(
		position: String,
		executor: (wrapper: ArenaKafkaMessageDto, kode: String) -> TiltakResult
	): TiltakResult

	fun createPayload(kode: String, navn: String): JsonNode {
		val data = ArenaTiltak(
			TILTAKSKODE = kode,
			TILTAKSNAVN = navn,
			TILTAKSGRUPPEKODE = GENERIC_STRING,
			REG_DATO = GENERIC_STRING,
			REG_USER = GENERIC_STRING,
			MOD_DATO = GENERIC_STRING,
			MOD_USER = GENERIC_STRING,
			DATO_FRA = GENERIC_STRING,
			DATO_TIL = GENERIC_STRING,
			AVSNITT_ID_GENERELT = GENERIC_INT,
			STATUS_BASISYTELSE = GENERIC_STRING,
			ADMINISTRASJONKODE = GENERIC_STRING,
			STATUS_KOPI_TILSAGN = GENERIC_STRING,
			ARKIVNOKKEL = GENERIC_STRING,
			STATUS_ANSKAFFELSE = GENERIC_STRING,
			MAKS_ANT_PLASSER = GENERIC_INT,
			MAKS_ANT_SOKERE = GENERIC_INT,
			STATUS_FAST_ANT_PLASSER = GENERIC_STRING,
			STATUS_SJEKK_ANT_DELTAKERE = GENERIC_STRING,
			STATUS_KALKULATOR = GENERIC_STRING,
			RAMMEAVTALE = GENERIC_STRING,
			OPPLAERINGSGRUPPE = GENERIC_STRING,
			HANDLINGSPLAN = GENERIC_STRING,
			STATUS_SLUTTDATO = GENERIC_STRING,
			MAKS_PERIODE = GENERIC_INT,
			STATUS_MELDEPLIKT = GENERIC_STRING,
			STATUS_VEDTAK = GENERIC_STRING,
			STATUS_IA_AVTALE = GENERIC_STRING,
			STATUS_TILLEGGSSTONADER = GENERIC_STRING,
			STATUS_UTDANNING = GENERIC_STRING,
			AUTOMATISK_TILSAGNSBREV = GENERIC_STRING,
			STATUS_BEGRUNNELSE_INNSOKT = GENERIC_STRING,
			STATUS_HENVISNING_BREV = GENERIC_STRING,
			STATUS_KOPIBREV = GENERIC_STRING
		)

		return objectMapper.readTree(objectMapper.writeValueAsString(data))
	}

}

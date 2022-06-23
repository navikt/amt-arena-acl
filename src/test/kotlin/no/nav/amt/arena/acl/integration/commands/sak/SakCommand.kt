package no.nav.amt.arena.acl.integration.commands.sak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaSak
import no.nav.amt.arena.acl.integration.commands.Command

abstract class SakCommand : Command() {

	abstract fun execute(
		position: String,
		executor: (wrapper: ArenaKafkaMessageDto, gjennomforingId: Long) -> SakResult
	): SakResult

	internal fun createPayload(input: SakInput) : JsonNode {
		val data = ArenaSak(
			SAK_ID = input.sakId,
			SAKSKODE = input.sakKode,
			REG_DATO = GENERIC_STRING,
			REG_USER = GENERIC_STRING,
			MOD_DATO = GENERIC_STRING,
			MOD_USER = GENERIC_STRING,
			TABELLNAVNALIAS = GENERIC_STRING,
			OBJEKT_ID = GENERIC_LONG,
			AAR = input.aar,
			LOPENRSAK = input.lopenr,
			DATO_AVSLUTTET = GENERIC_STRING,
			SAKSTATUSKODE = GENERIC_STRING,
			ARKIVNOKKEL = GENERIC_LONG,
			AETATENHET_ARKIV = GENERIC_STRING,
			ARKIVHENVISNING = GENERIC_STRING,
			BRUKERID_ANSVARLIG = GENERIC_STRING,
			AETATENHET_ANSVARLIG = input.ansvarligEnhetId,
			OBJEKT_KODE = GENERIC_STRING,
			STATUS_ENDRET = GENERIC_STRING,
			PARTISJON = GENERIC_LONG,
			ER_UTLAND = GENERIC_STRING,
		)
		return objectMapper.readTree(objectMapper.writeValueAsString(data))

	}
}

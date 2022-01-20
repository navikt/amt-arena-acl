package no.nav.amt.arena.acl.domain.arena

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import java.time.LocalDateTime
import java.time.Month

class ArenaWrapperTest : FunSpec({

	test("Deserialiser Deltaker (Update)") {
		val mapper = jacksonObjectMapper()
		val data = mapper.readValue(deltakerUpdate, ArenaWrapper::class.java)

		data.arenaId shouldBe "6412863"
		data.operation shouldBe ArenaOperation.U
		data.operationTimestamp shouldBe LocalDateTime.of(2022, Month.JANUARY, 6, 9, 0, 17)
		data.operationPosition shouldBe "00000000040360104892"

		data.before shouldNotBe null
		data.before shouldNotBe "null"

		data.after shouldNotBe null
		data.after shouldNotBe "null"
	}

	test("til ArenaData modell") {
		val mapper = jacksonObjectMapper()

		val wrapper = mapper.readValue(deltakerUpdate, ArenaWrapper::class.java)
		val arenaData = wrapper.toArenaData()

		arenaData.arenaTableName shouldBe wrapper.table
		arenaData.arenaId shouldBe wrapper.arenaId
		arenaData.operation shouldBe AmtOperation.MODIFIED
	}


})


const val deltakerUpdate = """
	{
	  "table": "SIAMO.TILTAKDELTAKER",
	  "op_type": "U",
	  "op_ts": "2022-01-06 09:00:17.000000",
	  "current_ts": "2022-01-06 09:00:21.461001",
	  "pos": "00000000040360104892",
	  "before": {
	    "TILTAKDELTAKER_ID": 6412863,
	    "PERSON_ID": 4865831,
	    "TILTAKGJENNOMFORING_ID": 3715121,
	    "DELTAKERSTATUSKODE": "AKTUELL",
	    "DELTAKERTYPEKODE": "INNSOKT",
	    "AARSAKVERDIKODE_STATUS": null,
	    "OPPMOTETYPEKODE": null,
	    "PRIORITET": null,
	    "BEGRUNNELSE_INNSOKT": null,
	    "BEGRUNNELSE_PRIORITERING": null,
	    "REG_DATO": "2022-01-06 09:00:11",
	    "REG_USER": "BHD0219",
	    "MOD_DATO": "2022-01-06 09:00:11",
	    "MOD_USER": "BHD0219",
	    "DATO_SVARFRIST": null,
	    "DATO_FRA": "2021-08-19 00:00:00",
	    "DATO_TIL": "2021-11-19 00:00:00",
	    "BEGRUNNELSE_STATUS": null,
	    "PROSENT_DELTID": 22,
	    "BRUKERID_STATUSENDRING": "BHD0219",
	    "DATO_STATUSENDRING": "2022-01-06 09:00:11",
	    "AKTIVITET_ID": 133911746,
	    "BRUKERID_ENDRING_PRIORITERING": null,
	    "DATO_ENDRING_PRIORITERING": null,
	    "DOKUMENTKODE_SISTE_BREV": null,
	    "STATUS_INNSOK_PAKKE": null,
	    "STATUS_OPPTAK_PAKKE": null,
	    "OPPLYSNINGER_INNSOK": null,
	    "PARTISJON": null,
	    "BEGRUNNELSE_BESTILLING": null,
	    "ANTALL_DAGER_PR_UKE": null
	  },
	  "after": {
	    "TILTAKDELTAKER_ID": 6412863,
	    "PERSON_ID": 4865831,
	    "TILTAKGJENNOMFORING_ID": 3715121,
	    "DELTAKERSTATUSKODE": "AKTUELL",
	    "DELTAKERTYPEKODE": "INNSOKT",
	    "AARSAKVERDIKODE_STATUS": null,
	    "OPPMOTETYPEKODE": null,
	    "PRIORITET": null,
	    "BEGRUNNELSE_INNSOKT": "Syntetisert rettighet",
	    "BEGRUNNELSE_PRIORITERING": null,
	    "REG_DATO": "2022-01-06 09:00:11",
	    "REG_USER": "BHD0219",
	    "MOD_DATO": "2022-01-06 09:00:11",
	    "MOD_USER": "BHD0219",
	    "DATO_SVARFRIST": null,
	    "DATO_FRA": "2021-08-19 00:00:00",
	    "DATO_TIL": "2021-11-19 00:00:00",
	    "BEGRUNNELSE_STATUS": null,
	    "PROSENT_DELTID": 22,
	    "BRUKERID_STATUSENDRING": "BHD0219",
	    "DATO_STATUSENDRING": "2022-01-06 09:00:11",
	    "AKTIVITET_ID": 133911746,
	    "BRUKERID_ENDRING_PRIORITERING": null,
	    "DATO_ENDRING_PRIORITERING": null,
	    "DOKUMENTKODE_SISTE_BREV": null,
	    "STATUS_INNSOK_PAKKE": null,
	    "STATUS_OPPTAK_PAKKE": null,
	    "OPPLYSNINGER_INNSOK": null,
	    "PARTISJON": null,
	    "BEGRUNNELSE_BESTILLING": null,
	    "ANTALL_DAGER_PR_UKE": null
	  }
	}
"""

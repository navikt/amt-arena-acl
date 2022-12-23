package no.nav.amt.arena.acl.mocks

import no.nav.amt.arena.acl.clients.mr_arena_adapter.Gjennomforing
import no.nav.amt.arena.acl.clients.mr_arena_adapter.GjennomforingArenaData
import okhttp3.mockwebserver.MockResponse
import java.util.*

class MockMrArenaAdapterServer : MockHttpServer() {

	fun mockHentGjennomforing(id: UUID, gjennomforing: Gjennomforing) {
		val body = """
			{
				"id": "${gjennomforing.id}",
				"tiltak": {
					"id": "${gjennomforing.tiltak.id}",
					"navn": "${gjennomforing.tiltak.navn}",
					"arenaKode": "${gjennomforing.tiltak.arenaKode}"
				},
				"navn": "${gjennomforing.navn}",
				"startDato": "${gjennomforing.startDato}",
				"sluttDato": "${gjennomforing.sluttDato}"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)
		handleRequest(matchPath = "/TODO/${id}", response = response)
	}

	fun mockHentGjennomforingId(arenaId: String, gjennomforingId: UUID) {
		val body = """
			{
				"id": "$gjennomforingId"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)
		handleRequest(matchPath = "/TODO-2/${arenaId}", response = response)
	}

	fun mockHentGjennomforingArenaData(id: UUID, gjennomforingArenaData: GjennomforingArenaData) {
		val body = """
			{
				"opprettetAar": "${gjennomforingArenaData.opprettetAar}",
				"lopenr": ${gjennomforingArenaData.lopenr},
				"virksomhetsnummer": "${gjennomforingArenaData.virksomhetsnummer}",
				"ansvarligNavEnhetId": "${gjennomforingArenaData.ansvarligNavEnhetId}",
				"status": "${gjennomforingArenaData.status}"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)
		handleRequest(matchPath = "/TODO-3/${id}", response = response)
	}

}

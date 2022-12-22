package no.nav.amt.arena.acl.mocks

import no.nav.amt.arena.acl.clients.mr_arena_adapter.GjennomforingArenaData
import okhttp3.mockwebserver.MockResponse
import java.util.*

class MockMrArenaAdapterServer : MockHttpServer() {

	fun gjennomforingArenaData(id: UUID, arenaDataResponse: GjennomforingArenaData) {
		val body = """
			{
				"opprettetAar": ${arenaDataResponse.opprettetAar},
				"lopenr": ${arenaDataResponse.lopenr},
				"virksomhetsnummer": "${arenaDataResponse.virksomhetsnummer}",
				"ansvarligNavEnhetId": "${arenaDataResponse.ansvarligNavEnhetId}",
				"status": "${arenaDataResponse.status}"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)
		handleRequest(matchPath = "/TODO/${id}", response = response)
	}
}

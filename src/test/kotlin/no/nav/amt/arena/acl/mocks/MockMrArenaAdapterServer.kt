package no.nav.amt.arena.acl.mocks

import no.nav.amt.arena.acl.clients.mr_arena_adapter.Gjennomforing
import okhttp3.mockwebserver.MockResponse
import java.util.*

class MockMrArenaAdapterServer : MockHttpServer() {

	fun mockGjennomforing(id: UUID, gjennomforing: Gjennomforing) {
		val body = """
			{
				"id": "$gjennomforing.id",
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
}

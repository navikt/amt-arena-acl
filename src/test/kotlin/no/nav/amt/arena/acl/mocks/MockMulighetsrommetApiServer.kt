package no.nav.amt.arena.acl.mocks

import no.nav.amt.arena.acl.clients.mulighetsrommet_api.GjennomforingArenaData
import okhttp3.mockwebserver.MockResponse
import java.util.*

class MockMulighetsrommetApiServer : MockHttpServer() {

	fun mockHentGjennomforingId(arenaId: Long, gjennomforingId: UUID, responseCode: Int = 200) {
		val body = """
			{
				"id": "$gjennomforingId"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(responseCode).setBody(body)
		handleRequest(matchPath = "/api/v1/tiltaksgjennomforinger/id/${arenaId}", response = response)
	}

	fun mockHentGjennomforingArenaData(id: UUID, gjennomforingArenaData: GjennomforingArenaData) {
		val virksomhetsnummer = if (gjennomforingArenaData.virksomhetsnummer == null) "null" else "\"${gjennomforingArenaData.virksomhetsnummer}\""
		val body = """
			{
				"opprettetAar": "${gjennomforingArenaData.opprettetAar}",
				"lopenr": ${gjennomforingArenaData.lopenr},
				"virksomhetsnummer": $virksomhetsnummer,
				"ansvarligNavEnhetId": "${gjennomforingArenaData.ansvarligNavEnhetId}",
				"status": "${gjennomforingArenaData.status}"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)
		handleRequest(matchPath = "/api/v1/tiltaksgjennomforinger/arenadata/${id}", response = response)
	}

}

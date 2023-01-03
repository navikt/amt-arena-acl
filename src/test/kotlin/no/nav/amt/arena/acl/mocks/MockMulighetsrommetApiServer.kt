package no.nav.amt.arena.acl.mocks

import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.GjennomforingArenaData
import okhttp3.mockwebserver.MockResponse
import java.util.*

class MockMulighetsrommetApiServer : MockHttpServer() {

	fun mockHentGjennomforing(id: UUID, gjennomforing: Gjennomforing) {
		val body = """
			{
				"id": "${gjennomforing.id}",
				"tiltakstype": {
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
		handleRequest(matchPath = "/api/v1/tiltaksgjennomforinger/${id}", response = response)
	}

	fun mockHentGjennomforingId(arenaId: String, gjennomforingId: UUID) {
		val body = """
			{
				"id": "$gjennomforingId"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)
		handleRequest(matchPath = "/api/v1/tiltaksgjennomforinger/id/${arenaId}", response = response)
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
		handleRequest(matchPath = "/api/v1/tiltaksgjennomforinger/arenadata/${id}", response = response)
	}

}

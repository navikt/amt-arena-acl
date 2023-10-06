package no.nav.amt.arena.acl.mocks

import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
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

	fun mockHentGjennomforingData(id: UUID, gjennomforingData: Gjennomforing?) {
		val body = """
			{
				"id": "$id",
				"tiltakstype": {
					"id": "${UUID.randomUUID()}",
					"arenaKode": "INDOPPFAG",
					"navn": "navn på tiltakstype"
				},
				"navn": "navn på gjennomføring",
				"status": "GJENNOMFORES",
				"startDato": "${gjennomforingData?.startDato}",
				"sluttDato": "${gjennomforingData?.sluttDato}",
				"virksomhetsnummer": "${gjennomforingData?.virksomhetsnummer}",
				"oppstart": "LOPENDE"
			}
		""".trimIndent()

		val response = if(gjennomforingData != null) MockResponse().setResponseCode(200).setBody(body) else MockResponse().setResponseCode(404).setBody("{}")
		handleRequest(matchPath = "/api/v1/tiltaksgjennomforinger/$id", response = response)
	}

}

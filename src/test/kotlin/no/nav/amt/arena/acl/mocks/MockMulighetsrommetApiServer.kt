package no.nav.amt.arena.acl.mocks

import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import okhttp3.mockwebserver.MockResponse
import java.util.UUID

class MockMulighetsrommetApiServer : MockHttpServer() {

	fun mockHentGjennomforingId(arenaId: Long, gjennomforingId: UUID, responseCode: Int = 200) {
		val body = """{"id": "$gjennomforingId"}"""

		val response = MockResponse().setResponseCode(responseCode).setBody(body)
		handleRequest(matchPath = "/api/v1/tiltaksgjennomforinger/id/${arenaId}", response = response)
	}

	fun mockHentGjennomforingV2Data(id: UUID, gjennomforingData: Gjennomforing?) {
		val tiltakskode = Tiltakskode.OPPFOLGING.name
		val body = """
			{
				"id": "$id",
				"tiltakskode": "$tiltakskode",
				"tiltakstype": {
					"tiltakskode": "$tiltakskode"
				},
				"arrangor": {
					"organisasjonsnummer": "${gjennomforingData?.virksomhetsnummer}"
				}
			}
		""".trimIndent()

		val response = if (gjennomforingData != null) MockResponse().setResponseCode(200)
			.setBody(body) else MockResponse().setResponseCode(404).setBody("{}")
		handleRequest(matchPath = "/api/v2/tiltaksgjennomforinger/$id", response = response)
	}
}

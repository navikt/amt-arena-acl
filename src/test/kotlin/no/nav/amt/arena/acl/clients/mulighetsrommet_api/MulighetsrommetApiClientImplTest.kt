package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.mocks.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import java.time.LocalDate
import java.util.*

class MulighetsrommetApiClientImplTest : FunSpec({

	val server = MockHttpServer(true)

	val client = MulighetsrommetApiClientImpl(
		baseUrl = server.serverUrl(),
		tokenProvider = { "TOKEN" },
	)

	afterEach {
		server.reset()
	}

	test("hentGjennomforingId - skal lage riktig request og parse respons") {
		val id = UUID.randomUUID()
		val arenaId = "1234"

		server.handleRequest(
			response = MockResponse().setBody(
				"""
					{
						"id": "$id"
					}
				""".trimIndent()
			)
		)
		val returnedId = client.hentGjennomforingId(arenaId)
		val request = server.latestRequest()

		returnedId shouldBe id

		request.path shouldBe "/api/v1/tiltaksgjennomforinger/id/$arenaId"
		request.method shouldBe "GET"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
	}

	test("hentGjennomforingId - skal returnere null hvis status er 404") {
		server.handleRequest(response = MockResponse().setResponseCode(404))

		val returnedId = client.hentGjennomforingId("1234")
		returnedId shouldBe null
	}

	test("hentGjennomforingArenaData - skal lage riktig request og parse respons") {
		val id = UUID.randomUUID()
		val startDato = LocalDate.now()
		val virksomhetsnummer = "8530254"
		server.handleRequest(
			response = MockResponse().setBody(
				"""
					{
						"id": "$id",
						"tiltakstype": {
							"id": "${UUID.randomUUID()}",
							"navn": "tiltaksnavn",
							"arenaKode": "INDOPPFAG"
						},
						"navn": "navn på gjennomføring",
						"status": "GJENNOMFORES",
						"startDato": "$startDato",
						"sluttDato": null,
						"virksomhetsnummer": "$virksomhetsnummer"
					}
				""".trimIndent()
			)
		)

		val gjennomforingArenaData = client.hentGjennomforing(id)

		val request = server.latestRequest()

		gjennomforingArenaData.virksomhetsnummer shouldBe virksomhetsnummer
		gjennomforingArenaData.status shouldBe Gjennomforing.Status.GJENNOMFORES
		gjennomforingArenaData.startDato shouldBe startDato

		request.path shouldBe "/api/v1/tiltaksgjennomforinger/$id"
		request.method shouldBe "GET"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
	}
})


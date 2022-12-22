package no.nav.amt.arena.acl.clients.mr_arena_adapter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.LocalDate
import java.util.*

class MulighetsrommetArenaClientImplTest : FunSpec({

	val server = MockWebServer()
	val serverUrl = server.url("").toString().removeSuffix("/")

	afterSpec {
		server.shutdown()
	}

	test("hentGjennomforing - skal lage riktig request og parse respons") {
		val client = MrArenaAdapterClientImpl(
			baseUrl = serverUrl,
			tokenProvider = { "TOKEN" },
		)

		val id = UUID.randomUUID()
		val tiltakId = UUID.randomUUID()
		val tiltakNavn = "Tiltak 1"
		val tiltakArenaKode = "KODE"
		val navn = "Gjennomforing 1"
		val startDato = "2022-12-22"
		val sluttDato = "2023-12-22"

		server.enqueue(
			MockResponse().setBody(
				"""
					{
						"id": "$id",
						"tiltak": {
							"id": "$tiltakId",
							"navn": "$tiltakNavn",
							"arenaKode": "$tiltakArenaKode"
						},
						"navn": "$navn",
						"startDato": "$startDato",
						"sluttDato": "$sluttDato"
					}
				""".trimIndent()
			)
		)

		val gjennomforing = client.hentGjennomforing(id)

		val request = server.takeRequest()

		gjennomforing.id shouldBe id
		gjennomforing.navn shouldBe navn
		gjennomforing.startDato shouldBe LocalDate.parse(startDato)
		gjennomforing.sluttDato shouldBe LocalDate.parse(sluttDato)

		gjennomforing.tiltak.id shouldBe tiltakId
		gjennomforing.tiltak.navn shouldBe tiltakNavn
		gjennomforing.tiltak.arenaKode shouldBe tiltakArenaKode


		request.path shouldBe "/TODO/$id"
		request.method shouldBe "GET"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
	}

	test("hentGjennomforingId - skal lage riktig request og parse respons") {
		val client = MrArenaAdapterClientImpl(
			baseUrl = serverUrl,
			tokenProvider = { "TOKEN" },
		)

		val id = UUID.randomUUID()
		val arenaId = "1234"

		server.enqueue(
			MockResponse().setBody(
				"""
					{
						"id": "$id"
					}
				""".trimIndent()
			)
		)
		val returnedId = client.hentGjennomforingId(arenaId)
		val request = server.takeRequest()

		returnedId shouldBe id

		request.path shouldBe "/TODO-2/$arenaId"
		request.method shouldBe "GET"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
	}

	test("hentGjennomforingId - skal returnere null hvis status er 404") {
		val client = MrArenaAdapterClientImpl(
			baseUrl = serverUrl,
			tokenProvider = { "TOKEN" },
		)

		server.enqueue(
			MockResponse().setResponseCode(404)
		)

		val returnedId = client.hentGjennomforingId("1234")
		returnedId shouldBe null
	}

	test("hentGjennomforingArenaData - skal lage riktig request og parse respons") {
		val client = MrArenaAdapterClientImpl(
			baseUrl = serverUrl,
			tokenProvider = { "TOKEN" },
		)

		server.enqueue(
			MockResponse().setBody(
				"""
					{
						"opprettetAar": 2022,
						"lopenr": 123,
						"virksomhetsnummer": "999222333",
						"ansvarligNavEnhetId": "1234",
						"status": "GJENNOMFORES"
					}
				""".trimIndent()
			)
		)

		val id = UUID.randomUUID()
		val gjennomforingArenaData = client.hentGjennomforingArenaData(id)

		val request = server.takeRequest()

		gjennomforingArenaData.opprettetAar shouldBe 2022
		gjennomforingArenaData.lopenr shouldBe 123
		gjennomforingArenaData.virksomhetsnummer shouldBe "999222333"
		gjennomforingArenaData.ansvarligNavEnhetId shouldBe "1234"
		gjennomforingArenaData.status shouldBe "GJENNOMFORES"

		request.path shouldBe "/TODO/$id"
		request.method shouldBe "GET"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
	}
})


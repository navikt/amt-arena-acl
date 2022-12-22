package no.nav.amt.arena.acl.clients.ordsproxy

import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ArenaOrdsProxyClientImplTest {
	var token = "TOKEN"

	val server = MockWebServer()
	val serverUrl = server.url("").toString().removeSuffix("/")



	@Test
	fun `hentFnr() skal lage riktig request og parse respons`() {
		val client = ArenaOrdsProxyClientImpl(
			arenaOrdsProxyUrl = serverUrl,
			tokenProvider = { token },
		)

		server.enqueue(
			MockResponse().setBody(
				"""
					{
						"fnr": "78900"
					}
				""".trimIndent()
			)
		)
		val fnr = client.hentFnr("987654")

		val request = server.takeRequest()

		request.path shouldBe "/api/ords/fnr?personId=987654"
		request.method shouldBe "GET"
		request.getHeader("Authorization") shouldBe "Bearer $token"
		fnr shouldBe "78900"
	}

	@Test
	fun `hentFnr() skal null hvis stauts er 404`() {
		val client = ArenaOrdsProxyClientImpl(
			arenaOrdsProxyUrl = serverUrl,
			tokenProvider = { token },
		)

		server.enqueue(MockResponse().setResponseCode(404))

		client.hentFnr("987654") shouldBe null
	}

	@Test
	fun `hentArbeidsgiver() skal lage riktig request og parse respons`() {
		val client = ArenaOrdsProxyClientImpl(
			arenaOrdsProxyUrl = serverUrl,
			tokenProvider = { token },
		)

		server.enqueue(
			MockResponse().setBody(
				"""
					{
						"virksomhetsnummer": "6834920",
						"organisasjonsnummerMorselskap": "74894532"
					}
				""".trimIndent()
			)
		)

		val arbeidsgiver = client.hentArbeidsgiver("1234567")

		val request = server.takeRequest()

		request.path shouldBe "/api/ords/arbeidsgiver?arbeidsgiverId=1234567"
		request.method shouldBe "GET"
		request.getHeader("Authorization") shouldBe "Bearer $token"

		val expectedArbeidsgiver = Arbeidsgiver(
			virksomhetsnummer = "6834920",
			organisasjonsnummerMorselskap = "74894532"
		)

		arbeidsgiver shouldBe expectedArbeidsgiver
	}

	@Test
	fun `hentArbeidsgiver() skal returnere null hvis status er 404`() {
		val client = ArenaOrdsProxyClientImpl(
			arenaOrdsProxyUrl = serverUrl,
			tokenProvider = { token },
		)

		server.enqueue(MockResponse().setResponseCode(404))

		client.hentArbeidsgiver("1234567") shouldBe null
	}
}

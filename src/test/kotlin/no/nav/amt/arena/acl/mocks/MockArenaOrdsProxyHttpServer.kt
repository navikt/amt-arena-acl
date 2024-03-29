package no.nav.amt.arena.acl.mocks

import okhttp3.mockwebserver.MockResponse


class MockArenaOrdsProxyHttpServer : MockHttpServer() {

	fun mockHentFnr(arenaPersonId: Long, fnr: String) {
		val body = """
			{
				"fnr": "$fnr"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)
		handleRequest(matchPath = "/api/ords/fnr?personId=$arenaPersonId", response = response)
	}

	fun mockHentFnr(fnr: String) {
		val body = """
			{
				"fnr": "$fnr"
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)

		handleRequest(matchRegexPath = "^/api/ords/fnr.*".toRegex(), response = response)
	}

	fun mockFailHentFnr(arenaPersonId: Long) {
		val response = MockResponse().setResponseCode(500)
		handleRequest(matchPath = "/api/ords/fnr?personId=$arenaPersonId", response = response)
	}

	fun mockFailure(code: Int = 500) {
		handleRequest(response = MockResponse().setResponseCode(code))
	}


	fun mockHentVirksomhetsnummer(arenaArbeidsgiverId: String, virksomhetsnummer: String) {
		val body = """
			{
				"virksomhetsnummer": "$virksomhetsnummer",
				"organisasjonsnummerMorselskap": ""
			}
		""".trimIndent()

		val response = MockResponse().setResponseCode(200).setBody(body)
		handleRequest(matchPath = "/api/ords/arbeidsgiver?arbeidsgiverId=$arenaArbeidsgiverId", response = response)
	}
}

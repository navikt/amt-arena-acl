package no.nav.amt.arena.acl.mocks

import okhttp3.mockwebserver.MockResponse


class MockArenaOrdsProxyHttpServer : MockHttpServer() {

	fun mockHentFnr(arenaPersonId: String, fnr: String?) {
		val fnrString = if (fnr == null) "null" else "\"$fnr\""
		val body = """
			{
				"fnr": $fnrString
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

	fun mockFailure() {
		handleRequest(response = MockResponse().setResponseCode(500))
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

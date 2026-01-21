package no.nav.amt.arena.acl.mocks

import no.nav.amt.arena.acl.clients.amttiltak.DeltakerDto
import no.nav.amt.arena.acl.clients.amttiltak.DeltakerStatus
import no.nav.amt.arena.acl.utils.JsonUtils.objectMapper
import okhttp3.mockwebserver.MockResponse
import java.time.LocalDate
import java.util.UUID

class MockAmtTiltakServer : MockHttpServer() {
	fun mockHentDeltakelserForPerson(
		deltakerId: UUID?,
		gjennomforingId: UUID,
		startdato: LocalDate?,
		sluttdato: LocalDate?,
		status: DeltakerStatus = DeltakerStatus.FULLFORT,
		responseCode: Int = 200,
	) {
		val body =
			deltakerId?.let {
				"""
				[
				  {
					"id": "$deltakerId",
					"gjennomforing": {
					  "id": "$gjennomforingId"
					},
					"startDato": "$startdato",
					"sluttDato": "$sluttdato",
					"status": "${status.name}"
				  }
				]
				""".trimIndent()
			} ?: objectMapper.writeValueAsString(emptyList<DeltakerDto>())

		val response = MockResponse().setResponseCode(responseCode).setBody(body)
		handleRequest(matchPath = "/api/external/deltakelser", response = response)
	}
}

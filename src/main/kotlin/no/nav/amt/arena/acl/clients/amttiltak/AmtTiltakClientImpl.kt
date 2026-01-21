package no.nav.amt.arena.acl.clients.amttiltak

import no.nav.amt.arena.acl.utils.JsonUtils.objectMapper
import no.nav.common.rest.client.RestClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import tools.jackson.module.kotlin.readValue
import java.util.function.Supplier

class AmtTiltakClientImpl(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = RestClient.baseClient(),
) {
	private val mediaTypeJson = "application/json".toMediaType()

	fun hentDeltakelserForPerson(personIdent: String): List<DeltakerDto> {
		val request =
			Request
				.Builder()
				.url("$baseUrl/api/external/deltakelser")
				.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider.get()}")
				.post(objectMapper.writeValueAsString(HentDeltakelserRequest(personIdent)).toRequestBody(mediaTypeJson))
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente tiltaksdeltakelser fra amt-tiltak. status=${response.code}")
			}

			val body = response.body.string()
			return objectMapper.readValue<List<DeltakerDto>>(body)
		}
	}

	private data class HentDeltakelserRequest(
		val personIdent: String,
	)
}

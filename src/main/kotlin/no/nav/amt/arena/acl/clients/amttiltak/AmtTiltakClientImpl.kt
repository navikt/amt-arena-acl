package no.nav.amt.arena.acl.clients.amttiltak

import jakarta.ws.rs.core.HttpHeaders
import no.nav.amt.arena.acl.utils.JsonUtils
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import no.nav.common.rest.client.RestClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.function.Supplier

class AmtTiltakClientImpl(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = RestClient.baseClient()
) : AmtTiltakClient {
	override fun hentDeltakelserForPerson(personIdent: String): List<DeltakerDto> {
		val request = Request.Builder()
			.url("$baseUrl/api/external/deltakelser")
			.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider.get()}")
			.post(toJsonString(HentDeltakelserRequest(personIdent))
				.toRequestBody("application/json".toMediaType()))
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente tiltaksdeltakelser fra amt-tiltak. status=${response.code}")
			}

			return JsonUtils.fromJsonString<List<DeltakerDto>>(response.body.string())
		}
	}
}

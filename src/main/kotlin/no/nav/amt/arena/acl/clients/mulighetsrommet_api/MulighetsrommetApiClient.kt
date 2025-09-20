package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.util.UUID
import java.util.function.Supplier

class MulighetsrommetApiClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {
	fun hentGjennomforingId(arenaId: String): UUID? {
		val request = Request.Builder()
			.url("$baseUrl/api/v1/tiltaksgjennomforinger/id/$arenaId")
			.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == 404) {
				return null
			}
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente gjennomføring arenadata fra Mulighetsrommet. status=${response.code}")
			}

			val responseBody = fromJsonString<HentGjennomforingIdResponse>(response.body.string())
			return responseBody.id
		}
	}

	fun hentGjennomforing(id: UUID): Gjennomforing {
		val request = Request.Builder()
			.url("$baseUrl/api/v1/tiltaksgjennomforinger/$id")
			.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente gjennomføring arenadata fra Mulighetsrommet. status=${response.code}")
			}

			return fromJsonString(response.body.string())
		}
	}
}

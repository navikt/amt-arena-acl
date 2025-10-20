package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.function.Supplier

class MulighetsrommetApiClientImpl(
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient = baseClient(),
) : MulighetsrommetApiClient {

	override fun hentGjennomforingId(arenaId: String): UUID? {
		val request = Request.Builder()
			.url("$baseUrl/api/v1/tiltaksgjennomforinger/id/$arenaId")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == 404) {
				return null
			}
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente gjennomføring arenadata fra Mulighetsrommet. status=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val responseBody = fromJsonString<HentGjennomforingId.Response>(body)

			return responseBody.id
		}
	}

	override fun hentGjennomforing(id: UUID): Gjennomforing {
		val request = Request.Builder()
			.url("$baseUrl/api/v1/tiltaksgjennomforinger/$id")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente gjennomføring fra Mulighetsrommet. status=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			return fromJsonString(body)

		}
	}

	override fun hentGjennomforingV2(id: UUID): Gjennomforing {
		val request = Request.Builder()
			.url("$baseUrl/api/v2/tiltaksgjennomforinger/$id")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente gjennomføring fra Mulighetsrommet v2 API. status=${response.code}")
			}

			val body = response.body.string()
			val responseBody = fromJsonString<GjennomforingV2Response>(body)
			return responseBody.toGjennomforing()

		}
	}

	object HentGjennomforingId{
		data class Response(
			val id: UUID
		)
	}

}

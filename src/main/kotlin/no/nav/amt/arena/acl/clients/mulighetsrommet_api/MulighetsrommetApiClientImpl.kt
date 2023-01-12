package no.nav.amt.arena.acl.clients.mulighetsrommet_api

import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.util.*
import java.util.function.Supplier

class MulighetsrommetApiClientImpl(
    private val baseUrl: String,
    private val tokenProvider: Supplier<String>,
    private val httpClient: OkHttpClient = baseClient(),
) : MulighetsrommetApiClient {

	override fun hentGjennomforing(id: UUID): Gjennomforing {
		val request = Request.Builder()
			.url("$baseUrl/api/v1/tiltaksgjennomforinger/$id")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente gjennomføring arenadata fra Mulighetsrommet. status=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val responseBody = fromJsonString<HentGjennomforing.Response>(body)

			return Gjennomforing(
				id = responseBody.id,
				tiltak = responseBody.tiltakstype.let { Tiltakstype(
					id = it.id,
					navn = it.navn,
					arenaKode = it.arenaKode,
				) },
				navn = responseBody.navn,
				startDato = responseBody.startDato,
				sluttDato = responseBody.sluttDato,
			)
		}
	}

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

	override fun hentGjennomforingArenaData(id: UUID): GjennomforingArenaData {
		val request = Request.Builder()
			.url("$baseUrl/api/v1/tiltaksgjennomforinger/arenadata/$id")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente gjennomføring arenadata fra Mulighetsrommet. status=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val responseBody = fromJsonString<HentGjennomforingArenaData.Response>(body)

			return GjennomforingArenaData(
				opprettetAar = responseBody.opprettetAar,
				lopenr = responseBody.lopenr,
				virksomhetsnummer = responseBody.virksomhetsnummer,
				ansvarligNavEnhetId = responseBody.ansvarligNavEnhetId,
				status = responseBody.status,
			)
		}
	}

	object HentGjennomforingArenaData {
		data class Response(
			val opprettetAar: Int,
			val lopenr: Int,
			val virksomhetsnummer: String?,
			val ansvarligNavEnhetId: String,
			val status: String
		)
	}

	object HentGjennomforingId{
		data class Response(
			val id: UUID
		)
	}

	object HentGjennomforing{
		data class Response(
			val id: UUID,
			val tiltakstype: TiltakstypeDto,
			val navn: String?,
			val startDato: LocalDate? = null,
			val sluttDato: LocalDate? = null,
		)

		data class TiltakstypeDto(
			val id: UUID,
			val navn: String,
			val arenaKode: String
		)
	}


}

package no.nav.amt.arena.acl.clients.mulighetsrommet

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.amt.arena.acl.utils.JsonUtils.objectMapper
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import tools.jackson.module.kotlin.readValue
import java.util.UUID
import java.util.function.Supplier

class MulighetsrommetApiClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {
	fun hentGjennomforingId(arenaId: String): UUID? {
		val request =
			Request
				.Builder()
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

			val body = response.body.string()

			val responseBody = objectMapper.readValue<HentGjennomforingIdResponse>(body)

			return responseBody.id
		}
	}

	fun hentGjennomforing(id: UUID): Gjennomforing {
		val request =
			Request
				.Builder()
				.url("$baseUrl/api/v1/tiltaksgjennomforinger/$id")
				.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider.get()}")
				.get()
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente gjennomføring fra Mulighetsrommet. status=${response.code}")
			}

			val body = response.body.string()

			return objectMapper.readValue(body)
		}
	}

	fun hentGjennomforingV2(id: UUID): Gjennomforing {
		val request =
			Request
				.Builder()
				.url("$baseUrl/api/v2/tiltaksgjennomforinger/$id")
				.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider.get()}")
				.get()
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente gjennomføring fra Mulighetsrommet v2 API. status=${response.code}")
			}

			val body = response.body.string()
			val responseBody = objectMapper.readValue<GjennomforingV2Response>(body)
			return responseBody.toGjennomforing()
		}
	}

	private data class HentGjennomforingIdResponse(
		val id: UUID,
	)

	private data class GjennomforingV2Response(
		val id: UUID,
		val tiltakskode: String? = null, // skal gjøres non-nullable
		val tiltakstype: TiltakstypeResponse? = null, // skal fjernes
		val arrangor: ArrangorResponse,
	) {
		data class ArrangorResponse(
			val organisasjonsnummer: String,
		)

		data class TiltakstypeResponse(
			val tiltakskode: String,
		)

		// erstattes av tiltakskode senere
		@get:JsonIgnore
		val effectiveTiltakskode: String
			get() = tiltakskode ?: tiltakstype?.tiltakskode ?: throw IllegalStateException("Tiltakskode er ikke satt")

		fun toGjennomforing(): Gjennomforing {
			val arenaKode =
				Tiltakskode
					.valueOf(effectiveTiltakskode)
					.toArenaKode()
					.name

			return Gjennomforing(
				id = id,
				tiltakstype = Gjennomforing.Tiltakstype(arenaKode = arenaKode),
				virksomhetsnummer = arrangor.organisasjonsnummer,
			)
		}
	}
}

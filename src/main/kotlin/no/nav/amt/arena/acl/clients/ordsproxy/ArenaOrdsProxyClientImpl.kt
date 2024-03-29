package no.nav.amt.arena.acl.clients.ordsproxy

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpStatus
import java.util.function.Supplier

open class ArenaOrdsProxyClientImpl(
	private val arenaOrdsProxyUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) : ArenaOrdsProxyClient {

	override fun hentFnr(arenaPersonId: String): String? {
		val request = Request.Builder()
			.url("$arenaOrdsProxyUrl/api/ords/fnr?personId=$arenaPersonId")
			.header("Authorization", "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == HttpStatus.NOT_FOUND.value()) {
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente fnr for Arena personId. Status: ${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			return fromJsonString<HentFnrResponse>(body).fnr
		}
	}

	override fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String {
		val request = Request.Builder()
			.url("$arenaOrdsProxyUrl/api/ords/arbeidsgiver?arbeidsgiverId=$arenaArbeidsgiverId")
			.header("Authorization", "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == HttpStatus.NOT_FOUND.value()) {
				throw NoSuchElementException("Fant ikke arbeidsgiver for id $arenaArbeidsgiverId")
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente arbeidsgiver. Status: ${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val arbeidsgiverResponse = fromJsonString<ArbeidsgiverResponse>(body)

			return arbeidsgiverResponse.virksomhetsnummer
		}
	}

	private data class HentFnrResponse(
		val fnr: String,
	)

	private data class ArbeidsgiverResponse(
		val virksomhetsnummer: String,
		val organisasjonsnummerMorselskap: String,
	)

}

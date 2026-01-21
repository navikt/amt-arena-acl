package no.nav.amt.arena.acl.clients.ordsproxy

import no.nav.amt.arena.acl.utils.JsonUtils.objectMapper
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import tools.jackson.module.kotlin.readValue
import java.util.function.Supplier

class ArenaOrdsProxyClient(
	private val arenaOrdsProxyUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {
	fun hentFnr(arenaPersonId: String): String? {
		val request =
			Request
				.Builder()
				.url("$arenaOrdsProxyUrl/api/ords/fnr?personId=$arenaPersonId")
				.header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider.get()}")
				.get()
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == HttpStatus.NOT_FOUND.value()) {
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente fnr for Arena personId. Status: ${response.code}")
			}

			val body = response.body.string()

			return objectMapper.readValue<HentFnrResponse>(body).fnr
		}
	}

	fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String {
		val request =
			Request
				.Builder()
				.url("$arenaOrdsProxyUrl/api/ords/arbeidsgiver?arbeidsgiverId=$arenaArbeidsgiverId")
				.header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider.get()}")
				.get()
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == HttpStatus.NOT_FOUND.value()) {
				throw NoSuchElementException("Fant ikke arbeidsgiver for id $arenaArbeidsgiverId")
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente arbeidsgiver. Status: ${response.code}")
			}

			val body = response.body.string()

			val arbeidsgiverResponse = objectMapper.readValue<ArbeidsgiverResponse>(body)

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

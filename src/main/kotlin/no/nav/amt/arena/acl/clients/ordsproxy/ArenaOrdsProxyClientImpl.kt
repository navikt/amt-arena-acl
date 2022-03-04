package no.nav.amt.arena.acl.clients.ordsproxy

import ArenaOrdsProxyClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpStatus
import java.util.function.Supplier

data class Arbeidsgiver(
	val virksomhetsnummer: String,
	val organisasjonsnummerMorselskap: String
)

open class ArenaOrdsProxyClientImpl(
	private val arenaOrdsProxyUrl: String,
	private val proxyTokenProvider: Supplier<String>,
	private val ordsProxyTokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
	private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule(),
) : ArenaOrdsProxyClient {

	override fun hentFnr(arenaPersonId: String): String? {
		val request = Request.Builder()
			.url("$arenaOrdsProxyUrl/api/ords/fnr?personId=$arenaPersonId")
			.header("Downstream-Authorization", "Bearer ${ordsProxyTokenProvider.get()}")
			.header("Authorization", "Bearer ${proxyTokenProvider.get()}")
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

			return objectMapper.readValue(body, HentFnrResponse::class.java).fnr
		}
	}

	override fun hentArbeidsgiver(arenaArbeidsgiverId: String): Arbeidsgiver? {
		val request = Request.Builder()
			.url("$arenaOrdsProxyUrl/api/ords/arbeidsgiver?arbeidsgiverId=$arenaArbeidsgiverId")
			.header("Downstream-Authorization", "Bearer ${ordsProxyTokenProvider.get()}")
			.header("Authorization", "Bearer ${proxyTokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == HttpStatus.NOT_FOUND.value()) {
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente arbeidsgiver. Status: ${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val arbeidsgiverResponse = objectMapper.readValue(body, ArbeidsgiverResponse::class.java)

			return Arbeidsgiver(
				virksomhetsnummer = arbeidsgiverResponse.virksomhetsnummer,
				organisasjonsnummerMorselskap = arbeidsgiverResponse.organisasjonsnummerMorselskap
			)
		}
	}

	override fun hentVirksomhetsnummer(arenaArbeidsgiverId: String): String {
		return hentArbeidsgiver(arenaArbeidsgiverId)?.virksomhetsnummer
			?: throw UnsupportedOperationException("Kan ikke hente virksomhetsnummer på en arbeidsgiver som ikke eksisterer. arenaArbeidsgiverId=$arenaArbeidsgiverId")
	}

	private data class HentFnrResponse(
		val fnr: String,
	)

	private data class ArbeidsgiverResponse(
		val virksomhetsnummer: String,
		val organisasjonsnummerMorselskap: String,
	)

}

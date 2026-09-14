package no.nav.amt.arena.acl.clients.mulighetsrommet

import no.nav.amt.arena.acl.utils.JsonUtils.objectMapper
import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.ARBEIDSRETTET_REHABILITERING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.AVKLARING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.HOYERE_UTDANNING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.JOBBKLUBB
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.OPPFOLGING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET
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
		val tiltakskode: Tiltakskode, // skal gjøres non-nullable
		val arrangor: ArrangorResponse,
		val oppstart: Oppstartstype,
	) {
		data class ArrangorResponse(
			val organisasjonsnummer: String,
		)

		fun toGjennomforing(): Gjennomforing {
			val arenaKode = tiltakskode.toArenaKodeLocal()

			return Gjennomforing(
				id = id,
				tiltakstype = Gjennomforing.Tiltakstype(arenaKode = arenaKode.name),
				virksomhetsnummer = arrangor.organisasjonsnummer,
				oppstart = oppstart
			)
		}
	}
}

fun Tiltakskode.toArenaKodeLocal() =
	when (this) {
		ARBEIDSFORBEREDENDE_TRENING -> ArenaKode.ARBFORB
		ARBEIDSRETTET_REHABILITERING -> ArenaKode.ARBRRHDAG
		AVKLARING -> ArenaKode.AVKLARAG
		DIGITALT_OPPFOLGINGSTILTAK -> ArenaKode.DIGIOPPARB
		GRUPPE_ARBEIDSMARKEDSOPPLAERING -> ArenaKode.GRUPPEAMO
		GRUPPE_FAG_OG_YRKESOPPLAERING -> ArenaKode.GRUFAGYRKE
		JOBBKLUBB -> ArenaKode.JOBBK
		OPPFOLGING -> ArenaKode.INDOPPFAG
		VARIG_TILRETTELAGT_ARBEID_SKJERMET -> ArenaKode.VASV
		ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING -> ArenaKode.ENKELAMO
		ENKELTPLASS_FAG_OG_YRKESOPPLAERING -> ArenaKode.ENKFAGYRKE
		HOYERE_UTDANNING -> ArenaKode.HOYEREUTD
		Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> ArenaKode.GRUPPEAMO
		Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> ArenaKode.GRUPPEAMO
		Tiltakskode.STUDIESPESIALISERING -> ArenaKode.GRUPPEAMO
		Tiltakskode.FAG_OG_YRKESOPPLAERING -> ArenaKode.GRUFAGYRKE
		Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING -> ArenaKode.GRUFAGYRKE
		Tiltakskode.TILPASSET_JOBBSTOTTE -> throw UnsupportedOperationException("Tilpasset jobbstøtte deltakelser skal ikke komme fra arena")
	}

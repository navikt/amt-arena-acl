package no.nav.amt.arena.acl

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.integration.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalManagementPort
import org.springframework.http.HttpStatus
import org.springframework.web.util.UriComponentsBuilder
import kotlin.jvm.java

class ActuatorTest(
	@LocalManagementPort private val managementPort: Int,
	private val restTemplate: TestRestTemplate,
) : IntegrationTestBase() {
	@ParameterizedTest(name = "{0} probe skal returnere OK og status = UP")
	@ValueSource(strings = ["liveness", "readiness"])
	fun probe_skal_returnere_OK_og_status_UP(probeName: String) {
		val uri =
			UriComponentsBuilder
				.fromUriString("http://localhost:{port}/internal/health/{probeName}")
				.buildAndExpand(managementPort, probeName)
				.toUri()

		val response = restTemplate.getForEntity(uri, String::class.java)

		assertSoftly(response) {
			statusCode shouldBe HttpStatus.OK
			body shouldBe "{\"status\":\"UP\"}"
		}
	}

	@Test
	fun `Prometheus-endepunktet skal returnere OK`() {
		val uri =
			UriComponentsBuilder
				.fromUriString("http://localhost:{port}/internal/prometheus")
				.buildAndExpand(managementPort)
				.toUri()

		val response = restTemplate.getForEntity(uri, String::class.java)

		response.statusCode shouldBe HttpStatus.OK
	}

	@Test
	fun `Metrics-endepunktet skal returnere NOT_FOUND`() {
		val uri =
			UriComponentsBuilder
				.fromUriString("http://localhost:{port}/internal/metrics")
				.buildAndExpand(managementPort)
				.toUri()

		val response = restTemplate.getForEntity(uri, String::class.java)

		response.statusCode shouldBe HttpStatus.NOT_FOUND
	}
}

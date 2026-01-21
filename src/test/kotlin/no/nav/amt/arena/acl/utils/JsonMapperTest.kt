package no.nav.amt.arena.acl.utils

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.TestConstructor
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.time.Year

/**
 * Tester for å verifisere at den statisk opprettede [ObjectMapper] som brukes i integrasjonstester
 * oppfører seg på samme måte som den Spring-injectede [ObjectMapper] i applikasjonen.
 *
 * Testene dekker:
 * 1. At serialisering av et DTO-objekt gir identisk JSON for både statisk og Spring-mapper.
 * 2. At den statiske mapperen ignorerer ukjente JSON-felt ved deserialisering, på samme måte som Spring-mapperen.
 *
 * Hensikten er å sikre konsistens mellom mapperen som brukes i produksjon og mapperen som brukes i testene,
 * slik at tester ikke blir falsk positive/falsk negative på grunn av forskjeller i konfigurering.
 */
@JsonTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JsonMapperTest(
	private val springObjectMapper: ObjectMapper,
) {
	@Test
	fun `static mapper skal serialisere samme som Spring mapper`() {
		val dto = TestDto("John Doe", 42, now)

		val jsonStatic = JsonUtils.objectMapper.writeValueAsString(dto)
		val jsonSpring = springObjectMapper.writeValueAsString(dto)

		val mapStatic: Map<String, Any> = JsonUtils.objectMapper.readValue(jsonStatic)
		val mapSpring: Map<String, Any> = springObjectMapper.readValue(jsonSpring)

		mapStatic shouldBe mapSpring

		jsonSpring shouldBe """{"name":"John Doe","age":42,"timestamp":"${Year.now()}-11-23T12:34:56"}"""
	}

	@Test
	fun `static mapper should ignore unknown properties`() {
		val jsonWithExtra = """{"name":"John Doe","age":42,"timestamp":"${Year.now()}-11-23T12:34:56","extra":"ignored"}"""
		val deserialized = JsonUtils.objectMapper.readValue<TestDto>(jsonWithExtra)

		assertSoftly(deserialized) {
			name shouldBe "John Doe"
			age shouldBe 42
			timestamp shouldBe now
		}
	}

	companion object {
		private val now: LocalDateTime = LocalDateTime.of(Year.now().value, 11, 23, 12, 34, 56)

		private data class TestDto(
			val name: String,
			val age: Int,
			val timestamp: LocalDateTime,
		)
	}
}

package no.nav.amt.arena.acl.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.Year

class StringExtensionsTest : StringSpec({

	"asLocalDate - should parse valid date given date time string" {
		"${Year.now()}-01-01 12:34:56".asLocalDate() shouldBe LocalDate.of(Year.now().value, 1, 1)
	}

	"removeNullCharacters - should remove null characters" {
		"\u0000test\u0000".removeNullCharacters() shouldBe "test"
	}

	"removeNullCharacters - should remove escaped null characters" {
		"\\u0000test\\u0000".removeNullCharacters() shouldBe "test"
	}
})

package no.nav.amt.arena.acl.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StringUtilsTest : FunSpec({

	test("removeNullCharacters - should remove null characters") {
		"\u0000test\u0000".removeNullCharacters() shouldBe "test"
	}

	test("removeNullCharacters - should remove escaped null characters") {
		"\\u0000test\\u0000".removeNullCharacters() shouldBe "test"
	}

})

package no.nav.amt.arena.acl.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StringUtilsTest : FunSpec({

	test("removeNullCharacters - should remove null characters") {
		"""\\u0000test1\\u0000""".removeNullCharacters() shouldBe "test1"
		"\u0000test2\u0000".removeNullCharacters() shouldBe "test2"
	}

})

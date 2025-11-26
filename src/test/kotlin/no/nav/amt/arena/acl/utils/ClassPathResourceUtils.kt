package no.nav.amt.arena.acl.utils

import org.springframework.core.io.ClassPathResource

object ClassPathResourceUtils {
	fun readResourceAsText(path: String): String = ClassPathResource(path).file.readText()
}

package no.nav.amt.arena.acl.utils

import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader

object ClassPathResourceUtils {

	fun readResourceAsText(path: String): String =
		ClassPathResource(path).inputStream.bufferedReader().use(BufferedReader::readText)
}

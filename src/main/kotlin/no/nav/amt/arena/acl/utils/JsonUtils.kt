package no.nav.amt.arena.acl.utils

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

object JsonUtils {
	val objectMapper: ObjectMapper = jacksonObjectMapper()
}

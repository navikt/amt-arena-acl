package no.nav.amt.arena.acl.utils

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

object JsonUtils {
	val objectMapper: ObjectMapper =
		jacksonMapperBuilder()
			.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
			.build()
}

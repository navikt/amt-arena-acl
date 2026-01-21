package no.nav.amt.arena.acl.utils

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import tools.jackson.module.kotlin.treeToValue

object JsonUtils {
	val objectMapper: ObjectMapper =
		jacksonMapperBuilder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build()

	inline fun <reified T> fromJsonString(jsonStr: String): T = objectMapper.readValue(jsonStr)

	fun toJsonString(any: Any): String = objectMapper.writeValueAsString(any)

	fun toJsonNode(jsonStr: String): JsonNode = objectMapper.readTree(jsonStr)

	inline fun <reified T> fromJsonNode(jsonNode: JsonNode): T = objectMapper.treeToValue(jsonNode)
}

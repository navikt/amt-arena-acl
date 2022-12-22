package no.nav.amt.arena.acl.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.treeToValue

object JsonUtils {

	val objectMapper = jacksonObjectMapper()
		.registerKotlinModule()
		.registerModule(JavaTimeModule())
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
		.configure(MapperFeature.USE_STD_BEAN_NAMING, true)

	inline fun <reified T> fromJsonString(jsonStr: String): T {
		return objectMapper.readValue(jsonStr)
	}

	fun toJsonString(any: Any): String {
		return objectMapper.writeValueAsString(any)
	}

	fun toJsonNode(jsonStr: String): JsonNode {
		return objectMapper.readTree(jsonStr)
	}

	inline fun <reified T> fromJsonNode(jsonNode: JsonNode): T {
		return objectMapper.treeToValue(jsonNode)
	}
}

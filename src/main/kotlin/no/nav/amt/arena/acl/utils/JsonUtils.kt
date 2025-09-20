package no.nav.amt.arena.acl.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue

object JsonUtils {

	val objectMapper: ObjectMapper = jacksonMapperBuilder()
		.addModule(JavaTimeModule())
		.enable(MapperFeature.USE_STD_BEAN_NAMING)
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.build()

	inline fun <reified T> fromJsonString(jsonStr: String): T = objectMapper.readValue(jsonStr)

	fun toJsonString(any: Any): String = objectMapper.writeValueAsString(any)

	fun toJsonNode(jsonStr: String): JsonNode = objectMapper.readTree(jsonStr)

	inline fun <reified T> fromJsonNode(jsonNode: JsonNode): T = objectMapper.treeToValue(jsonNode)
}

package no.nav.amt.arena.acl.processors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.arena.ArenaTiltak
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import org.springframework.stereotype.Component

@Component
open class TiltakProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val repository: TiltakRepository,
) {

	private val objectMapper = jacksonObjectMapper()
		.registerModule(JavaTimeModule())
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

	fun handle(data: ArenaData) {
		val arenaTiltak = getMainObject(data)

		when (data.operation) {
			AmtOperation.CREATED, AmtOperation.MODIFIED -> {
				repository.upsert(
					kode = arenaTiltak.TILTAKSKODE,
					navn = arenaTiltak.TILTAKSNAVN
				)
			}
			AmtOperation.DELETED -> {
				repository.delete(
					kode = arenaTiltak.TILTAKSKODE
				)
			}
		}

		arenaDataRepository.upsert(data.markAsHandled())
	}

	private fun getMainObject(data: ArenaData): ArenaTiltak {
		return when (data.operation) {
			AmtOperation.CREATED -> jsonObject(data.after ?: throw IllegalStateException("After cannot be null"))
			AmtOperation.MODIFIED -> jsonObject(data.after ?: throw IllegalStateException("After cannot be null"))
			AmtOperation.DELETED -> jsonObject(data.before ?: throw IllegalStateException("Before cannot be null"))
		}
	}

	private fun jsonObject(node: JsonNode): ArenaTiltak {
		return objectMapper.treeToValue<ArenaTiltak>(node)!!
	}
}

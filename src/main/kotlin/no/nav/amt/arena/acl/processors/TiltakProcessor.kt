package no.nav.amt.arena.acl.processors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.arena.ArenaTiltak
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class TiltakProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val repository: TiltakRepository,
) {

	private val objectMapper = ObjectMapperFactory.get()

	private val logger = LoggerFactory.getLogger(javaClass)

	fun handle(data: ArenaData) {
		val arenaTiltak = getMainObject(data)

		if (data.operation == AmtOperation.DELETED) {
			logger.error("Implementation for delete elements are not implemented. Cannot handle arena id ${data.arenaId} from table ${data.arenaTableName} at position ${data.operationPosition}.")
			arenaDataRepository.upsert(data.markAsFailed("Implementation of DELETE is not implemented."))
		} else {
			repository.upsert(
				kode = arenaTiltak.TILTAKSKODE,
				navn = arenaTiltak.TILTAKSNAVN
			)

			arenaDataRepository.upsert(data.markAsHandled())
		}

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

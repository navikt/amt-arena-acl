package no.nav.amt.arena.acl.processors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.arena.ArenaTiltak
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.TiltakService
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class TiltakProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val tiltakService: TiltakService,
) {

	private val objectMapper = ObjectMapperFactory.get()

	private val log = LoggerFactory.getLogger(javaClass)

	fun handle(data: ArenaData) {
		val arenaTiltak = getMainObject(data)

		if (data.operation == AmtOperation.DELETED) {
			log.error("Implementation for delete elements are not implemented. Cannot handle arena id ${data.arenaId} from table ${data.arenaTableName} at position ${data.operationPosition}.")
			arenaDataRepository.upsert(data.markAsFailed("Implementation of DELETE is not implemented."))
		} else {
			val id = UUID.randomUUID()
			val kode = arenaTiltak.TILTAKSKODE
			val navn = arenaTiltak.TILTAKSNAVN

			tiltakService.upsert(
				id = UUID.randomUUID(),
				kode = arenaTiltak.TILTAKSKODE,
				navn = navn
			)

			arenaDataRepository.upsert(data.markAsHandled())

			log.info("Upsert av tiltak id=$id kode=$kode navn=$navn")
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
		return objectMapper.treeToValue(node)
	}
}

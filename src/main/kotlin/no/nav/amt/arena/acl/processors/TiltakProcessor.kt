package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaTiltakKafkaMessage
import no.nav.amt.arena.acl.exceptions.OperationNotImplementedException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.TiltakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class TiltakProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val tiltakService: TiltakService,
) : ArenaMessageProcessor<ArenaTiltakKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun handleArenaMessage(message: ArenaTiltakKafkaMessage) {
		val data = message.getData()

		if (message.operationType == AmtOperation.DELETED) {
			log.error("Implementation for delete elements are not implemented. Cannot handle arena id ${data.TILTAKSKODE} from table ${message.arenaTableName} at position ${message.operationPosition}")
			throw OperationNotImplementedException("Kan ikke håndtere tiltak med operation type DELETE")
		}

		val id = UUID.randomUUID()
		val kode = data.TILTAKSKODE
		val navn = data.TILTAKSNAVN

		tiltakService.upsert(
			id = UUID.randomUUID(),
			kode = data.TILTAKSKODE,
			navn = navn
		)

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(kode))

		log.info("Upsert av tiltak id=$id kode=$kode navn=$navn")
	}

}

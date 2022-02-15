package no.nav.amt.arena.acl.integration.executors

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.arena.ArenaOperation
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.fail
import org.slf4j.LoggerFactory

abstract class TestExecutor(
	private val kafkaProducer: KafkaProducerClientImpl<String, String>,
	private val arenaDataRepository: ArenaDataRepository,
	private val translationRepository: ArenaDataIdTranslationRepository
) {

	companion object {
		var position = 0
	}

	val objectMapper = ObjectMapperFactory.get()

	private val logger = LoggerFactory.getLogger(javaClass)

	fun incrementAndGetPosition(): String {
		return "${position++}"
	}

	fun sendKafkaMessage(topic: String, payload: String) {
		kafkaProducer.send(ProducerRecord(topic, payload))
	}

	fun getArenaData(table: String, operation: AmtOperation, position: String): ArenaData {
		var attempts = 0
		while (attempts < 10) {
			val data = arenaDataRepository.getAll()
			data.forEach { entry ->
				if (entry.arenaTableName == table && entry.operation == operation && entry.operationPosition == position) {
					logger.info("Fant Arena data i tabell $table med operasjon $operation og posisjon $position etter $attempts forsøk.")
					return entry
				}
			}

			Thread.sleep(250)
			attempts++
		}

		fail("Could not find Arena data in table $table with operation $operation and position $position")
	}

	fun getTranslation(table: String, arenaId: String): ArenaDataIdTranslation? {
		var attempts = 0
		while (attempts < 5) {
			val data = translationRepository.get(table, arenaId)

			if (data != null) {
				return data
			}

			Thread.sleep(250)
			attempts++
		}

		return null
	}


	fun ArenaOperation.toAmtOperation(): AmtOperation {
		return when (this) {
			ArenaOperation.I -> AmtOperation.CREATED
			ArenaOperation.U -> AmtOperation.MODIFIED
			ArenaOperation.D -> AmtOperation.DELETED
		}
	}
}

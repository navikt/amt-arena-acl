package no.nav.amt.arena.acl.goldengate

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ArenaMessageProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaDataIdTranslationRepository: ArenaDataIdTranslationRepository
) {

	private val log = LoggerFactory.getLogger(this::class.java)

	private val mapper = jacksonObjectMapper()

	fun handleArenaGoldenGateRecord(record: ConsumerRecord<String, String>) {
		val data = mapper.readValue(record.value(), ArenaWrapper::class.java).toArenaData()

		if (isArenaDataIrrelevant(data)) {
			log.info("Arena data is not relevant and will be ignored."
				+ " topic=${record.topic()} partition=${record.partition()} offset=${record.offset()}"
				+ " table=${data.arenaTableName} arenaId=${data.arenaId}"
			)

			return
		}

		arenaDataRepository.upsert(data)
	}

	private fun isArenaDataIrrelevant(arenaData: ArenaData): Boolean {
		val arenaDataTranslation = arenaDataIdTranslationRepository.get(arenaData.arenaTableName, arenaData.arenaId)
		return arenaDataTranslation != null && arenaDataTranslation.ignored
	}

}

package no.nav.amt.arena.acl.integration.executors

import no.nav.amt.arena.acl.domain.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerCommand
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerResult
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.TILTAK_DELTAKER_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import java.util.*

class DeltakerTestExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	translationRepository: ArenaDataIdTranslationRepository
) : TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
	translationRepository = translationRepository
) {

	private val topic = "deltaker"
	private val outputMessages = mutableListOf<AmtWrapper<AmtDeltaker>>()

	init {
		KafkaAmtIntegrationConsumer.subscribeDeltaker { outputMessages.add(it) }
	}

	fun execute(command: DeltakerCommand): DeltakerResult {
		return command.execute(incrementAndGetPosition()) { sendAndCheck(it) }
	}

	private fun sendAndCheck(wrapper: ArenaWrapper): DeltakerResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(wrapper))
		return getResults(wrapper)
	}

	private fun getResults(wrapper: ArenaWrapper): DeltakerResult {
		val arenaData = getArenaData(
			TILTAK_DELTAKER_TABLE_NAME,
			wrapper.operation.toAmtOperation(),
			wrapper.operationPosition
		)

		val translation = getTranslation(TILTAK_DELTAKER_TABLE_NAME, arenaData.arenaId)
		val message = if (translation != null) getOutputMessage(translation.amtId) else null

		return DeltakerResult(
			arenaData.operationPosition,
			arenaData,
			translation,
			message
		)
	}

	private fun getOutputMessage(id: UUID): AmtWrapper<AmtDeltaker>? {
		var attempts = 0
		while (attempts < 5) {
			val data = outputMessages.firstOrNull { it.payload != null && (it.payload as AmtDeltaker).id == id }

			if (data != null) {
				outputMessages.clear()
				return data
			}

			Thread.sleep(250)
			attempts++
		}

		outputMessages.clear()
		return null
	}

}

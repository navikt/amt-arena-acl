package no.nav.amt.arena.acl.integration.executors

import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerCommand
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerResult
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
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
	private val outputMessages = mutableListOf<AmtKafkaMessageDto<AmtDeltaker>>()

	init {
		KafkaAmtIntegrationConsumer.subscribeDeltaker { outputMessages.add(it) }
	}

	fun execute(command: DeltakerCommand): DeltakerResult {
		return command.execute(incrementAndGetPosition()) { sendAndCheck(it) }
	}

	fun updateResults(position: String, command: DeltakerCommand): DeltakerResult {
		return command.execute(position) { getResults(it) }
	}

	private fun sendAndCheck(wrapper: ArenaKafkaMessageDto): DeltakerResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(wrapper))
		return getResults(wrapper)
	}

	private fun getResults(wrapper: ArenaKafkaMessageDto): DeltakerResult {
		val arenaData = getArenaData(
			ARENA_DELTAKER_TABLE_NAME,
			AmtOperation.fromArenaOperationString(wrapper.opType),
			wrapper.pos
		)

		val translation = getTranslation(ARENA_DELTAKER_TABLE_NAME, arenaData.arenaId)
		val message = if (translation != null) getOutputMessage(translation.amtId) else null

		return DeltakerResult(
			arenaData.operationPosition,
			arenaData,
			translation,
			message
		)
	}

	private fun getOutputMessage(id: UUID): AmtKafkaMessageDto<AmtDeltaker>? {
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

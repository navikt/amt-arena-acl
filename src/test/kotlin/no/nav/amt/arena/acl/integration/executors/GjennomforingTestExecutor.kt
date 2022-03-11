package no.nav.amt.arena.acl.integration.executors

import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingCommand
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingResult
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import java.util.*

class GjennomforingTestExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	translationRepository: ArenaDataIdTranslationRepository
) : TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
	translationRepository = translationRepository
) {

	private val topic = "gjennomforing"

	private val outputMessages = mutableListOf<AmtKafkaMessageDto<AmtGjennomforing>>()

	init {
		KafkaAmtIntegrationConsumer.subscribeGjennomforing { outputMessages.add(it) }
	}

	fun execute(command: GjennomforingCommand): GjennomforingResult {
		return command.execute(incrementAndGetPosition()) { sendAndCheck(it) }
	}

	fun updateResults(position: String, command: GjennomforingCommand): GjennomforingResult {
		return command.execute(position) { getResults(it) }
	}

	private fun sendAndCheck(arenaWrapper: ArenaKafkaMessageDto): GjennomforingResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(arenaWrapper))
		return getResults(arenaWrapper)
	}

	private fun getResults(arenaWrapper: ArenaKafkaMessageDto): GjennomforingResult {
		val arenaData = getArenaData(
			ARENA_GJENNOMFORING_TABLE_NAME,
			AmtOperation.fromArenaOperationString(arenaWrapper.opType),
			arenaWrapper.pos
		)

		val translation = getTranslation(ARENA_GJENNOMFORING_TABLE_NAME, arenaData.arenaId)
		val message = if (translation != null) getOutputMessage(translation.amtId) else null

		return GjennomforingResult(arenaWrapper.pos, arenaData, translation, message)
	}


	private fun getOutputMessage(id: UUID): AmtKafkaMessageDto<AmtGjennomforing>? {
		var attempts = 0
		while (attempts < 5) {
			val data = outputMessages.firstOrNull { it.payload != null && (it.payload as AmtGjennomforing).id == id }

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

package no.nav.amt.arena.acl.integration.executors

import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingCommand
import no.nav.amt.arena.acl.integration.commands.gjennomforing.GjennomforingResult
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.TILTAKGJENNOMFORING_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import java.util.*

class GjennomforingTestExecutor(
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	private val translationRepository: ArenaDataIdTranslationRepository
) : TestExecutor(
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository
) {

	private val topic = "gjennomforing"

	private val outputMessages = mutableListOf<AmtWrapper<AmtGjennomforing>>()

	init {
		KafkaAmtIntegrationConsumer.subscribeGjennomforing { outputMessages.add(it) }
	}

	fun execute(command: GjennomforingCommand): GjennomforingResult {
		return command.execute(incrementAndGetPosition()) { sendAndCheck(it) }
	}

	fun updateResults(position: String, command: GjennomforingCommand): GjennomforingResult {
		return command.execute(position) { getResults(it) }
	}

	private fun sendAndCheck(arenaWrapper: ArenaWrapper): GjennomforingResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(arenaWrapper))
		return getResults(arenaWrapper)
	}

	private fun getResults(arenaWrapper: ArenaWrapper): GjennomforingResult {
		val arenaData = getArenaData(
			TILTAKGJENNOMFORING_TABLE_NAME,
			arenaWrapper.operation.toAmtOperation(),
			arenaWrapper.operationPosition
		)

		val translation = getTranslation(TILTAKGJENNOMFORING_TABLE_NAME, arenaData.arenaId)
		val message = if (translation != null) getOutputMessage(translation.amtId) else null

		return GjennomforingResult(arenaWrapper.operationPosition, arenaData, translation, message)
	}

	private fun getTranslation(table: String, arenaId: String): ArenaDataIdTranslation? {
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

	private fun getOutputMessage(id: UUID): AmtWrapper<AmtGjennomforing>? {
		var attempts = 0
		while (attempts < 5) {
			val data = outputMessages.firstOrNull { it.payload != null && (it.payload as AmtGjennomforing).id == id }

			if (data != null) {
				return data
			}

			Thread.sleep(250)
			attempts++
		}

		return null
	}


}

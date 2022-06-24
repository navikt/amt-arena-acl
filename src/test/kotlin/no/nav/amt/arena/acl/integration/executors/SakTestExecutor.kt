package no.nav.amt.arena.acl.integration.executors
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaKafkaMessageDto
import no.nav.amt.arena.acl.integration.commands.sak.SakCommand
import no.nav.amt.arena.acl.integration.commands.sak.SakResult
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.integration.utils.nullableAsyncRetryHandler
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.ArenaSakRepository
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_SAK_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.junit.jupiter.api.fail
import java.util.*

class SakTestExecutor (
	kafkaProducer: KafkaProducerClientImpl<String, String>,
	arenaDataRepository: ArenaDataRepository,
	translationRepository: ArenaDataIdTranslationRepository,
	val sakRepository: ArenaSakRepository
) : TestExecutor (
	kafkaProducer = kafkaProducer,
	arenaDataRepository = arenaDataRepository,
	translationRepository = translationRepository
) {

	private val topic = "sak"
	private val outputMessages = mutableListOf<AmtKafkaMessageDto<AmtGjennomforing>>()

	init {
		KafkaAmtIntegrationConsumer.subscribeGjennomforing { outputMessages.add(it) }
	}

	fun execute(command: SakCommand): SakResult {
		return command.execute(incrementAndGetPosition()) { wrapper, gjennomforingId -> sendAndCheck(wrapper, gjennomforingId) }
	}

	private fun sendAndCheck(arenaWrapper: ArenaKafkaMessageDto, gjennomforingId: Long): SakResult {
		sendKafkaMessage(topic, objectMapper.writeValueAsString(arenaWrapper))
		return getResults(arenaWrapper, gjennomforingId)
	}

	private fun getResults(arenaWrapper: ArenaKafkaMessageDto, gjennomforingId: Long): SakResult {
		val arenaData = getArenaData(
			ARENA_SAK_TABLE_NAME,
			AmtOperation.fromArenaOperationString(arenaWrapper.opType),
			arenaWrapper.pos
		)

		val storedSak = nullableAsyncRetryHandler({ sakRepository.hentSakMedArenaId(arenaData.arenaId.toLong()) })
			?: fail("Forventet at sak med id: ${arenaData.arenaId} ")

		val translation = getTranslation(ARENA_GJENNOMFORING_TABLE_NAME, gjennomforingId.toString())
		val message = if (translation != null) getOutputMessage(translation.amtId) else null

		return SakResult(arenaWrapper.pos, arenaData, translation, message, storedSak)
	}

	private fun getOutputMessage(id: UUID): AmtKafkaMessageDto<AmtGjennomforing>? {
		var attempts = 0
		while (attempts < 5) {
			val data = outputMessages.lastOrNull { it.payload != null && (it.payload as AmtGjennomforing).id == id }

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

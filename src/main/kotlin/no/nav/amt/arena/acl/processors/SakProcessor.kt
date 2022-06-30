package no.nav.amt.arena.acl.processors
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaSakKafkaMessage
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.ArenaGjennomforingRepository
import no.nav.amt.arena.acl.repositories.ArenaSakRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.services.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
open class SakProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val arenaSakRepository: ArenaSakRepository,
	private val arenaGjennomforingRepository: ArenaGjennomforingRepository,
	private val kafkaProducerService: KafkaProducerService,
	private val tiltakRepository: TiltakRepository
) : ArenaMessageProcessor<ArenaSakKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		private const val SAKSKODE_TILTAK = "TILT"
	}

	override fun handleArenaMessage(message: ArenaSakKafkaMessage) {
		val sak = message.getData().mapSak()

		if (sak.sakskode != SAKSKODE_TILTAK) {
			throw IgnoredException("Sak med kode ${sak.sakskode} er ikke relevant")
		}

		arenaSakRepository.upsertSak(
			arenaSakId = sak.sakId,
			aar = sak.aar,
			lopenr = sak.lopenr,
			ansvarligEnhetId = sak.ansvarligEnhetId
		)

		sendGjennomforing(sak.sakId, sak.lopenr, sak.aar, sak.ansvarligEnhetId)

		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(sak.sakId.toString()))
		log.info("Upsert av sak id=${sak.sakId}")

	}

	private fun sendGjennomforing(sakId: Long, lopenr: Int, opprettetAar: Int, ansvarligNavEnhetId: String?) {
		val gjennomforing = arenaGjennomforingRepository.getBySakId(sakId) ?: return
		val tiltak = tiltakRepository.getByKode(gjennomforing.tiltakKode) ?: throw IllegalStateException("Fant ikke tiltak med kode: ${gjennomforing.tiltakKode}")
		val nyGjennomforing = gjennomforing.copy(
			lopenr = lopenr,
			opprettetAar = opprettetAar,
			ansvarligNavEnhetId = ansvarligNavEnhetId
		)

		arenaGjennomforingRepository.upsert(nyGjennomforing)
		val kafkaMessage = AmtKafkaMessageDto(
			type = PayloadType.GJENNOMFORING,
			operation = AmtOperation.MODIFIED,
			payload = nyGjennomforing.toAmtGjennomforing(tiltak)
		)

		kafkaProducerService.sendTilAmtTiltak(gjennomforing.id, kafkaMessage)

		log.info("Melding for gjennomføring id=${gjennomforing.id} transactionId=${kafkaMessage.transactionId} op=${kafkaMessage.operation} er sendt")

	}

}

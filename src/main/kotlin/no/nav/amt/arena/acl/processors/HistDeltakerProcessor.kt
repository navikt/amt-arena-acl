package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import no.nav.amt.arena.acl.clients.amttiltak.AmtTiltakClient
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.Gjennomforing
import no.nav.amt.arena.acl.domain.db.toUpsertInputWithStatusHandled
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaHistDeltaker
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaHistDeltakerKafkaMessage
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.tryRun
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
open class HistDeltakerProcessor(
	private val arenaDataRepository: ArenaDataRepository,
	private val ordsClient: ArenaOrdsProxyClient,
	private val kafkaProducerService: KafkaProducerService,
	private val deltakerProcessor: DeltakerProcessor,
	private val amtTiltakClient: AmtTiltakClient
) : ArenaMessageProcessor<ArenaHistDeltakerKafkaMessage> {

	private val log = LoggerFactory.getLogger(javaClass)

	// OBS: Hvis denne skal brukes på nytt må man legge til noe som lagrer amt-id i arenaDataIdTranslationRepository
	// hvis den ikke finnes fra før, siden aktivitetskort-publisher henter arenaid for å slå opp aktivitetsid
	override fun handleArenaMessage(message: ArenaHistDeltakerKafkaMessage) {
		val arenaDeltakerRaw = message.getData()
		val arenaDeltakerId = arenaDeltakerRaw.HIST_TILTAKDELTAKER_ID.toString()
		val arenaGjennomforingId = arenaDeltakerRaw.TILTAKGJENNOMFORING_ID.toString()

		if (!arenaDeltakerRaw.EKSTERN_ID.isNullOrEmpty()) {
			throw IgnoredException("Ignorerer hist-deltaker som har eksternid ${arenaDeltakerRaw.EKSTERN_ID}")
		}
		if (arenaDeltakerRaw.DELTAKERTYPEKODE == "EKSTERN") {
			throw IgnoredException("Ignorerer hist-deltaker som har deltakertypekode ekstern, arenaid $arenaDeltakerId")
		}

		val gjennomforing = deltakerProcessor.getGjennomforing(arenaGjennomforingId)
		val deltaker = createDeltaker(arenaDeltakerRaw, gjennomforing)

		if (message.operationType != AmtOperation.CREATED) {
			log.info("Mottatt melding for hist-deltaker id=${deltaker.id} arenaHistId=$arenaDeltakerId op=${message.operationType}, blir ikke behandlet")
			throw IgnoredException("Ignorerer hist-deltaker som har operation type ${message.operationType}")
		} else {
			val deltakerKafkaMessage = AmtKafkaMessageDto(
				type = PayloadType.DELTAKER,
				operation = message.operationType,
				payload = deltaker
			)

			kafkaProducerService.sendTilAmtTiltak(deltaker.id, deltakerKafkaMessage)
			log.info("Melding for hist-deltaker id=${deltaker.id} arenaHistId=$arenaDeltakerId transactionId=${deltakerKafkaMessage.transactionId} op=${deltakerKafkaMessage.operation} er sendt")
		}
		arenaDataRepository.upsert(message.toUpsertInputWithStatusHandled(arenaDeltakerId))
	}

	private fun createDeltaker(arenaDeltakerRaw: ArenaHistDeltaker, gjennomforing: Gjennomforing): AmtDeltaker {
		val arenaDeltaker = arenaDeltakerRaw
			.tryRun { it.mapTiltakDeltaker() }
			.getOrThrow()

		val personIdent = ordsClient.hentFnr(arenaDeltaker.personId)
			?: throw ValidationException("Arena mangler personlig ident for personId=${arenaDeltaker.personId}")

		val deltakerIdFraAmtTiltak = getDeltakerId(
			personIdent = personIdent,
			gjennomforingId = gjennomforing.id,
			startdato = arenaDeltaker.datoFra,
			sluttdato = arenaDeltaker.datoTil
		)
		if (deltakerIdFraAmtTiltak != null) {
			throw IgnoredException("Hist-deltaker med arenaid ${arenaDeltakerRaw.HIST_TILTAKDELTAKER_ID} finnes i amt-tiltak med id $deltakerIdFraAmtTiltak")
		}
		return arenaDeltaker.constructDeltaker(
			amtDeltakerId = UUID.randomUUID(),
			gjennomforingId = gjennomforing.id,
			gjennomforingSluttDato = gjennomforing.sluttDato,
			erGjennomforingAvsluttet = gjennomforing.erAvsluttet(),
			erKurs = gjennomforing.erKurs(),
			personIdent = personIdent,
		)
	}

	private fun getDeltakerId(
		personIdent: String,
		gjennomforingId: UUID,
		startdato: LocalDate?,
		sluttdato: LocalDate?
	): UUID? {
		val deltakelser = amtTiltakClient.hentDeltakelserForPerson(personIdent)
		return deltakelser.find { it.gjennomforing.id == gjennomforingId && it.startDato == startdato && it.sluttDato == sluttdato }?.id
	}
}

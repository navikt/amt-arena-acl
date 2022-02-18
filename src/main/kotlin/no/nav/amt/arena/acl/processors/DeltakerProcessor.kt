package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.micrometer.core.instrument.MeterRegistry
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.Creation
import no.nav.amt.arena.acl.domain.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaTiltakDeltaker
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.TILTAKGJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.asLocalDate
import no.nav.amt.arena.acl.utils.asLocalDateTime
import no.nav.common.kafka.producer.KafkaProducerClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class DeltakerProcessor(
	repository: ArenaDataRepository,
	private val idTranslationRepository: ArenaDataIdTranslationRepository,
	private val ordsClient: ArenaOrdsProxyClient,
	meterRegistry: MeterRegistry,
	kafkaProducer: KafkaProducerClient<String, String>
) : AbstractArenaProcessor<ArenaTiltakDeltaker>(
	repository = repository,
	meterRegistry = meterRegistry,
	clazz = ArenaTiltakDeltaker::class.java,
	kafkaProducer = kafkaProducer
) {

	private val logger = LoggerFactory.getLogger(javaClass)
	private val statusConverter = DeltakerStatusConverter(meterRegistry)
	private val statusEndretDatoConverter = DeltakerEndretDatoConverter()

	override fun handleEntry(data: ArenaData) {
		val arenaDeltaker: ArenaTiltakDeltaker = data.getMainObject()
		val tiltakGjennomforingId = arenaDeltaker.TILTAKGJENNOMFORING_ID.toString()

		val gjennomforingInfo = idTranslationRepository.get(TILTAKGJENNOMFORING_TABLE_NAME, tiltakGjennomforingId)

		if (gjennomforingInfo == null) {
			logger.info("Tiltakgjennomføring $tiltakGjennomforingId er ikke håndtert, kan derfor ikke håndtere deltaker med Arena ID ${arenaDeltaker.TILTAKDELTAKER_ID} enda")
			repository.upsert(data.retry("Tiltakgjennomføring ($tiltakGjennomforingId) er ikke håndtert"))
			return
		} else if (gjennomforingInfo.ignored) {
			logger.info("Deltaker med med id ${arenaDeltaker.TILTAKDELTAKER_ID} er ikke støttet og sendes ikke videre")
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak"))
			return
		}

		val personIdent = ordsClient.hentFnr(arenaDeltaker.PERSON_ID.toString())
			?: throw IllegalStateException("Expected Person with ArenaId ${arenaDeltaker.PERSON_ID} to exist")

		val amtDeltakerId = idTranslationRepository.getAmtId(data.arenaTableName, data.arenaId)
			?: UUID.randomUUID()

		val amtDeltaker = arenaDeltaker.toAmtDeltaker(
			amtDeltakerId = amtDeltakerId,
			gjennomforingId = gjennomforingInfo.amtId,
			personIdent = personIdent
		)

		val deltakerInfo = insertTranslation(data.arenaTableName, data.arenaId, amtDeltaker)

		if (deltakerInfo.first == Creation.EXISTED) {
			val digest = getDigest(amtDeltaker)

			if (deltakerInfo.second.currentHash == digest) {
				logger.info("Deltaker med kode $amtDeltakerId sendes ikke videre fordi det allerede er sendt (Samme hash)")
				repository.upsert(data.markAsIgnored("Deltaker er allerede sendt (samme hash)."))
				return
			}
		}

		val amtData = AmtWrapper(
			type = "DELTAKER",
			operation = data.operation,
			payload = arenaDeltaker.toAmtDeltaker(amtDeltakerId, gjennomforingInfo.amtId, personIdent)
		)

		send(amtDeltaker.id, objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsHandled())
		logger.info(
			"[Transaction id: ${amtData.transactionId}] [Operation: ${amtData.operation}]"
				+ " [Gjennomføring: ${amtDeltaker.gjennomforingId}] Deltaker with id $amtDeltakerId sent"
		)
	}

	private fun insertTranslation(
		table: String,
		arenaId: String,
		deltaker: AmtDeltaker,
	): Pair<Creation, ArenaDataIdTranslation> {
		val exists = idTranslationRepository.get(table, arenaId)

		if (exists != null) {
			return Pair(Creation.EXISTED, exists)
		} else {
			idTranslationRepository.insert(
				ArenaDataIdTranslation(
					amtId = deltaker.id,
					arenaTableName = table,
					arenaId = arenaId,
					ignored = false,
					getDigest(deltaker)
				)
			)

			val created = idTranslationRepository.get(table, arenaId)
				?: throw IllegalStateException("Translation for id $arenaId in table $table should exist")

			return Pair(Creation.CREATED, created)
		}
	}

	private fun ArenaTiltakDeltaker.toAmtDeltaker(
		amtDeltakerId: UUID,
		gjennomforingId: UUID,
		personIdent: String
	): AmtDeltaker {
		var registrertDato = REG_DATO?.asLocalDateTime()

		if (registrertDato == null) {
			logger.warn("REG_DATO mangler for tiltakdeltaker arenaId=$TILTAKDELTAKER_ID amtId=${amtDeltakerId}, bruker MOD_DATO istedenfor")
			registrertDato = MOD_DATO.asLocalDateTime()
		}

		return AmtDeltaker(
			id = amtDeltakerId,
			gjennomforingId = gjennomforingId,
			personIdent = personIdent,
			startDato = DATO_FRA?.asLocalDate(),
			sluttDato = DATO_TIL?.asLocalDate(),
			status = statusConverter.convert(
				deltakerStatusCode = DELTAKERSTATUSKODE,
				deltakerRegistrertDato = registrertDato,
				startDato = DATO_FRA?.asLocalDate(),
				sluttDato = DATO_TIL?.asLocalDate(),
				datoStatusEndring = DATO_STATUSENDRING?.asLocalDate()
			),
			dagerPerUke = ANTALL_DAGER_PR_UKE,
			prosentDeltid = PROSENT_DELTID,
			registrertDato = registrertDato,
			statusEndretDato = statusEndretDatoConverter.convert(
				deltakerStatus = DELTAKERSTATUSKODE,
				datoStatusEndring = DATO_STATUSENDRING?.asLocalDateTime(),
				oppstartDato = DATO_FRA?.asLocalDate()?.atStartOfDay(),
				sluttDato = DATO_TIL?.asLocalDate()?.atStartOfDay()
			)
		)
	}


}

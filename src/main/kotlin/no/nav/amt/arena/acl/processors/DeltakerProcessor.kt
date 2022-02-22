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
import java.time.LocalDateTime
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

	private val log = LoggerFactory.getLogger(javaClass)
	private val statusConverter = DeltakerStatusConverter(meterRegistry)
	private val statusEndretDatoConverter = DeltakerEndretDatoConverter()

	override fun handleEntry(data: ArenaData) {
		val arenaDeltaker: ArenaTiltakDeltaker = data.getMainObject()

		val tiltakGjennomforingId = arenaDeltaker.TILTAKGJENNOMFORING_ID.toString()
		val deltakerArenaId = arenaDeltaker.TILTAKDELTAKER_ID.toString()
		val personId = arenaDeltaker.PERSON_ID?.toString()

		val gjennomforingInfo = idTranslationRepository.get(TILTAKGJENNOMFORING_TABLE_NAME, tiltakGjennomforingId)

		if (gjennomforingInfo == null) {
			log.info("Tiltakgjennomføring id=$tiltakGjennomforingId er ikke håndtert, kan derfor ikke håndtere deltaker med arenaId=$deltakerArenaId enda")
			repository.upsert(data.retry("Venter på at gjennomføring id=$tiltakGjennomforingId skal bli håndtert"))
			return
		} else if (gjennomforingInfo.ignored) {
			log.info("Ignorerer deltaker med arenaId=$deltakerArenaId som ikke er på et støttet tiltak")
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak"))
			return
		}

		if (personId == null) {
			log.warn("Ignorerer deltaker med arenaId=$deltakerArenaId som mangler PERSON_ID")
			repository.upsert(data.markAsIgnored("Mangler PERSON_ID"))
			return
		}

		val personIdent = ordsClient.hentFnr(personId)
			?: throw IllegalStateException("Expected Person with personId=$personId to exist")

		val deltakerAmtId = hentEllerOpprettNyDeltakerId(data.arenaTableName, data.arenaId)

		val amtDeltaker = arenaDeltaker.toAmtDeltaker(
			amtDeltakerId = deltakerAmtId,
			gjennomforingId = gjennomforingInfo.amtId,
			personIdent = personIdent
		)

		val deltakerInfo = insertTranslation(data.arenaTableName, data.arenaId, amtDeltaker)

		if (deltakerInfo.first == Creation.EXISTED) {
			val digest = getDigest(amtDeltaker)

			if (deltakerInfo.second.currentHash == digest) {
				log.info("Deltaker med kode id=$deltakerAmtId sendes ikke videre fordi det allerede er sendt (samme hash)")
				repository.upsert(data.markAsIgnored("Deltaker er allerede sendt (samme hash)"))
				return
			}
		}

		val amtData = AmtWrapper(
			type = "DELTAKER",
			operation = data.operation,
			payload = arenaDeltaker.toAmtDeltaker(deltakerAmtId, gjennomforingInfo.amtId, personIdent)
		)

		send(amtDeltaker.id, objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsHandled())
		log.info("Melding for deltaker id=$deltakerAmtId arenaId=$deltakerArenaId transactionId=${amtData.transactionId} op=${amtData.operation} er sendt")
	}

	private fun hentEllerOpprettNyDeltakerId(arenaTableName: String, arenaId: String): UUID {
		val deltakerId = idTranslationRepository.getAmtId(arenaTableName, arenaId)

		if (deltakerId == null) {
			val nyDeltakerIdId = UUID.randomUUID()
			log.info("Opprettet ny id for deltaker, id=$nyDeltakerIdId arenaId=$arenaId")
			return nyDeltakerIdId
		}

		return deltakerId
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

			log.info("Opprettet translation for deltaker id=${deltaker.id} arenaId=$arenaId")

			val created = idTranslationRepository.get(table, arenaId)
				?: throw IllegalStateException("Translation for id=${deltaker.id} arenaId=$arenaId in table $table should exist")

			return Pair(Creation.CREATED, created)
		}
	}

	private fun ArenaTiltakDeltaker.toAmtDeltaker(
		amtDeltakerId: UUID,
		gjennomforingId: UUID,
		personIdent: String
	): AmtDeltaker {
		val registrertDato = utledRegDato(this)

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

	private fun utledRegDato(arenaDeltaker: ArenaTiltakDeltaker): LocalDateTime {
		val registrertDato = arenaDeltaker.REG_DATO

		if (registrertDato != null) {
			return registrertDato.asLocalDateTime()
		}

		val modifisertDato = arenaDeltaker.MOD_DATO

		if (modifisertDato != null) {
			log.warn("REG_DATO mangler for tiltakdeltaker arenaId=${arenaDeltaker.TILTAKGJENNOMFORING_ID}, bruker MOD_DATO istedenfor")
			return modifisertDato.asLocalDateTime()
		}

		log.warn("MOD_DATO mangler for tiltakdeltaker arenaId=${arenaDeltaker.TILTAKGJENNOMFORING_ID}, bruker nåtid istedenfor")

		return LocalDateTime.now()
	}


}

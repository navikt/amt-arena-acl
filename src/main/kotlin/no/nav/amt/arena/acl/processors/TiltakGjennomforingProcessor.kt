package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.micrometer.core.instrument.MeterRegistry
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.Creation
import no.nav.amt.arena.acl.domain.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaTiltakGjennomforing
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.utils.asLocalDate
import no.nav.amt.arena.acl.utils.asLocalDateTime
import no.nav.amt.arena.acl.utils.asTime
import no.nav.amt.arena.acl.utils.withTime
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.util.*

@Component
open class TiltakGjennomforingProcessor(
	repository: ArenaDataRepository,
	private val idTranslationRepository: ArenaDataIdTranslationRepository,
	private val tiltakRepository: TiltakRepository,
	private val ordsClient: ArenaOrdsProxyClient,
	meterRegistry: MeterRegistry,
	kafkaProducer: KafkaProducerClientImpl<String, String>
) : AbstractArenaProcessor<ArenaTiltakGjennomforing>(
	repository = repository,
	meterRegistry = meterRegistry,
	clazz = ArenaTiltakGjennomforing::class.java,
	kafkaProducer = kafkaProducer
) {

	private val logger = LoggerFactory.getLogger(javaClass)
	private val statusConverter = GjennomforingStatusConverter()

	override fun handleEntry(data: ArenaData) {
		val arenaGjennomforing = getMainObject(data)

		val gjennomforingId = idTranslationRepository.getAmtId(data.arenaTableName, data.arenaId)
			?: UUID.randomUUID()

		if (ugyldigGjennomforing(arenaGjennomforing)) {
			logger.info("Hopper over upsert av tiltakgjennomforing som mangler data. arenaTiltakgjennomforingId=${arenaGjennomforing.TILTAKGJENNOMFORING_ID}")
			insertTranslation(data, gjennomforingId, true)
			repository.upsert(data.markAsIgnored())
			return
		}

		if (isIgnored(arenaGjennomforing)) {
			logger.info("Gjennomføring med id ${arenaGjennomforing.TILTAKGJENNOMFORING_ID} er ikke støttet og sendes ikke videre")
			insertTranslation(data, gjennomforingId, isIgnored(arenaGjennomforing))
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak."))
			return
		}

		val tiltakskode = arenaGjennomforing.TILTAKSKODE
		val tiltak = tiltakRepository.getByKode(tiltakskode)

		if (tiltak == null) {
			logger.info("Tiltak $tiltakskode er ikke håndtert, kan derfor ikke håndtere gjennomføring med Arena ID ${arenaGjennomforing.TILTAKGJENNOMFORING_ID} enda.")
			repository.upsert(data.retry("Tiltaket ($tiltakskode) er ikke håndtert"))
			return
		}

		val virksomhetsnummer = ordsClient.hentVirksomhetsnummer(arenaGjennomforing.ARBGIV_ID_ARRANGOR.toString())

		val amtGjennomforing = arenaGjennomforing.toAmtGjennomforing(
			amtTiltak = tiltak,
			amtGjennomforingId = gjennomforingId,
			virksomhetsnummer = virksomhetsnummer
		)

		val translation = insertTranslation(data, gjennomforingId, isIgnored(arenaGjennomforing))

		if (translation.first == Creation.EXISTED) {
			val digest = getDigest(amtGjennomforing)

			if (translation.second.currentHash == digest) {
				logger.info("Gjennomføring med kode $gjennomforingId sendes ikke videre fordi det allerede er sendt (Samme hash)")
				repository.upsert(data.markAsIgnored("Tiltaket er allerede sendt (samme hash)."))
				return
			}
		}

		val amtData = AmtWrapper(
			type = "GJENNOMFORING",
			operation = data.operation,
			payload = arenaGjennomforing.toAmtGjennomforing(tiltak, gjennomforingId, virksomhetsnummer)
		)

		send(amtGjennomforing.id, objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsHandled())
		logger.info("[Transaction id: ${amtData.transactionId}] [Operation: ${amtData.operation}] Gjennomføring with id $gjennomforingId Sent.")
	}

	private fun isIgnored(gjennomforing: ArenaTiltakGjennomforing): Boolean {
		return !isSupportedTiltak(gjennomforing.TILTAKSKODE)
	}

	private fun insertTranslation(
		data: ArenaData,
		gjennomforingId: UUID,
		ignored: Boolean
	): Pair<Creation, ArenaDataIdTranslation> {
		val exists = idTranslationRepository.get(data.arenaTableName, data.arenaId)

		if (exists != null) {
			return Pair(Creation.EXISTED, exists)
		} else {
			idTranslationRepository.insert(
				ArenaDataIdTranslation(
					amtId = gjennomforingId,
					arenaTableName = data.arenaTableName,
					arenaId = data.arenaId,
					ignored = ignored,
					getDigest(gjennomforingId)
				)
			)

			val created = idTranslationRepository.get(data.arenaTableName, data.arenaId)
				?: throw IllegalStateException("Translation for id ${data.arenaId} in table ${data.arenaTableName} should exist")

			return Pair(Creation.CREATED, created)
		}
	}

	private fun ArenaTiltakGjennomforing.toAmtGjennomforing(
		amtTiltak: AmtTiltak,
		amtGjennomforingId: UUID,
		virksomhetsnummer: String
	): AmtGjennomforing {
		return AmtGjennomforing(
			id = amtGjennomforingId,
			tiltak = amtTiltak,
			virksomhetsnummer = virksomhetsnummer,
			navn = LOKALTNAVN ?: throw DataIntegrityViolationException("Forventet at LOKALTNAVN ikke er null"),
			startDato = DATO_FRA?.asLocalDate(),
			sluttDato = DATO_TIL?.asLocalDate(),
			registrertDato = REG_DATO.asLocalDateTime(),
			fremmoteDato = DATO_FREMMOTE?.asLocalDate() withTime KLOKKETID_FREMMOTE.asTime(),
			status = statusConverter.convert(TILTAKSTATUSKODE)
		)
	}

	private fun ugyldigGjennomforing(data: ArenaTiltakGjennomforing) =
		data.ARBGIV_ID_ARRANGOR == null || data.LOKALTNAVN == null
}

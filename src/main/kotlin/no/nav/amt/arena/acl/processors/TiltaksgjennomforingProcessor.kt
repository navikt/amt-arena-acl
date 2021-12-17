package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.Creation
import no.nav.amt.arena.acl.domain.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaTiltakGjennomforing
import no.nav.amt.arena.acl.ordsproxy.ArenaOrdsProxyClient
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.*
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.util.*

@Component
open class TiltaksgjennomforingProcessor(
	private val repository: ArenaDataRepository,
	private val idTranslationRepository: ArenaDataIdTranslationRepository,
	private val ordsClient: ArenaOrdsProxyClient,
	kafkaProducer: KafkaProducerClientImpl<String, String>
) : AbstractArenaProcessor<ArenaTiltakGjennomforing>(
	clazz = ArenaTiltakGjennomforing::class.java,
	kafkaProducer = kafkaProducer
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	override fun handle(data: ArenaData) {
		val arenaGjennomforing = getMainObject(data)

		val tiltakskode = arenaGjennomforing.TILTAKSKODE

		val id = idTranslationRepository.getAmtId(data.arenaTableName, data.arenaId)
			?: UUID.randomUUID()

		//	TODO	val virksomhetsnummer = ordsClient.hentVirksomhetsnummer(arenaGjennomforing.ARBGIV_ID_ARRANGOR.toString())
		val virksomhetsnummer = "12345678910"

		val tiltakInfo = idTranslationRepository.get(TILTAK_TABLE_NAME, tiltakskode)

		if (tiltakInfo == null) {
			logger.debug("Tiltak $tiltakskode er ikke håndtert, kan derfor ikke håndtere gjennomføring med Arena ID ${arenaGjennomforing.TILTAKGJENNOMFORING_ID} enda.")
			repository.upsert(data.retry("Tiltaket ($tiltakskode) er ikke håndtert"))
			return
		}

		val amtGjennomforing = arenaGjennomforing.toAmtGjennomforing(
			amtTiltakId = tiltakInfo.amtId,
			amtGjennomforingId = id,
			virksomhetsnummer = virksomhetsnummer
		)

		if (isIgnored(arenaGjennomforing)) {
			logger.debug("Gjennomføring med kode $tiltakskode er ikke støttet og sendes ikke videre")
			getTranslation(data, amtGjennomforing, isIgnored(arenaGjennomforing))
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak."))
			return
		}

		val translation = getTranslation(data, amtGjennomforing, isIgnored(arenaGjennomforing))

		if (translation.first == Creation.EXISTED) {
			val digest = getDigest(amtGjennomforing)

			if (translation.second.currentHash == digest) {
				logger.info("Gjennomføring med kode $id sendes ikke videre fordi det allerede er sendt (Samme hash)")
				repository.upsert(data.markAsIgnored("Tiltaket er allerede sendt (samme hash)."))
				return
			}
		}

		val amtData = AmtWrapper(
			type = "GJENNOMFORING",
			operation = data.operation,
			before = data.before?.toAmtTiltak(tiltakInfo.amtId, id, virksomhetsnummer),
			after = data.after?.toAmtTiltak(tiltakInfo.amtId, id, virksomhetsnummer)
		)

		send(objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsSent())
		logger.info("[Transaction id: ${amtData.transactionId}] [Operation: ${amtData.operation}] Gjennomføring with id $id Sent.")
	}

	private fun isIgnored(gjennomforing: ArenaTiltakGjennomforing): Boolean {
		return !isSupportedTiltak(gjennomforing.TILTAKSKODE)
	}

	private fun getTranslation(
		data: ArenaData,
		gjennomforing: AmtGjennomforing,
		ignored: Boolean
	): Pair<Creation, ArenaDataIdTranslation> {
		val exists = idTranslationRepository.get(data.arenaTableName, data.arenaId)

		if (exists != null) {
			return Pair(Creation.EXISTED, exists)
		} else {
			idTranslationRepository.insert(
				ArenaDataIdTranslation(
					amtId = gjennomforing.id,
					arenaTableName = data.arenaTableName,
					arenaId = data.arenaId,
					ignored = ignored,
					getDigest(gjennomforing)
				)
			)

			val created = idTranslationRepository.get(data.arenaTableName, data.arenaId)
				?: throw IllegalStateException("Translation for id ${data.arenaId} in table ${data.arenaTableName} should exist")

			return Pair(Creation.CREATED, created)
		}
	}


	private fun String.toAmtTiltak(
		amtTiltakId: UUID,
		amtGjennomforingId: UUID,
		virksomhetsnummer: String
	): AmtGjennomforing {
		return jsonObject(this, ArenaTiltakGjennomforing::class.java)?.toAmtGjennomforing(
			amtTiltakId,
			amtGjennomforingId,
			virksomhetsnummer
		)
			?: throw IllegalArgumentException("Expected String not to be null")
	}


	private fun ArenaTiltakGjennomforing.toAmtGjennomforing(
		amtTiltakId: UUID,
		amtGjennomforingId: UUID,
		virksomhetsnummer: String
	): AmtGjennomforing {
		return AmtGjennomforing(
			id = amtGjennomforingId,
			tiltakId = amtTiltakId,
			virksomhetsnummer = virksomhetsnummer,
			navn = LOKALTNAVN ?: throw DataIntegrityViolationException("Forventet at LOKALTNAVN ikke er null"),
			oppstartDato = DATO_FRA?.asLocalDate(),
			sluttDato = DATO_TIL?.asLocalDate(),
			registrert = REG_DATO.asLocalDateTime(),
			fremmote = DATO_FREMMOTE?.asLocalDate() withTime KLOKKETID_FREMMOTE.asTime()
		)
	}
}

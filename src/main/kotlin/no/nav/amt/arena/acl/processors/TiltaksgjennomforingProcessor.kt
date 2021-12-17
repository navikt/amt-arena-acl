package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
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
		val arenaGjennomforingId = arenaGjennomforing.TILTAKGJENNOMFORING_ID.toString()

		if (!isSupportedTiltak(tiltakskode)) {
			logger.debug("Tiltaksgjennomføring for tiltak med kode $tiltakskode er ikke støttet og sendes ikke videre")
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak."))
			return
		}

		val tiltakIdInfo = idTranslationRepository.get(TILTAK_TABLE_NAME, tiltakskode)

		if (tiltakIdInfo == null) {
			logger.debug("Tiltak $tiltakskode er ikke håndtert, kan derfor ikke håndtere gjennomføring med Arena ID ${arenaGjennomforing.TILTAKGJENNOMFORING_ID} enda.")
			repository.upsert(data.retry("Tiltaket ($tiltakskode) er ikke håndtert"))
			return
		}

//		val virksomhetsnummer = ordsClient.hentVirksomhetsnummer(arenaGjennomforing.ARBGIV_ID_ARRANGOR.toString())
		val virksomhetsnummer = "12345678910"
		var gjennomforingIdInfo = idTranslationRepository.get(TILTAKGJENNOMFORING_TABLE_NAME, arenaGjennomforingId)


		if (gjennomforingIdInfo != null) {
			val amtGjennomforing = arenaGjennomforing.toAmtGjennomforing(
				amtTiltakId = tiltakIdInfo.amtId,
				amtGjennomforingId = gjennomforingIdInfo.amtId,
				virksomhetsnummer = virksomhetsnummer
			)

			val digest = getDigest(amtGjennomforing)

			if (gjennomforingIdInfo.currentHash == digest) {
				logger.info("Tiltaksgjennomforing med id ${gjennomforingIdInfo.amtId} sendes ikke videre fordi det allerede er sendt (Samme hash)")
				repository.upsert(data.markAsIgnored("Gjennomføringen er allerede sendt (samme hash)."))
				return
			}
		} else {
			gjennomforingIdInfo = generateTranslation(tiltakIdInfo.amtId, virksomhetsnummer, data, arenaGjennomforing)
		}

		val amtData = AmtWrapper(
			type = "GJENNOMFORING",
			operation = data.operation,
			before = data.before?.toAmtTiltak(tiltakIdInfo.amtId, gjennomforingIdInfo.amtId, virksomhetsnummer),
			after = data.after?.toAmtTiltak(tiltakIdInfo.amtId, gjennomforingIdInfo.amtId, virksomhetsnummer)
		)

		send(objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsSent())
		logger.info("[Transaction id: ${amtData.transactionId}] [Operation: ${amtData.operation}] Gjennomføring with id ${gjennomforingIdInfo.amtId} Sent.")

	}

	private fun generateTranslation(
		tiltakId: UUID,
		virksomhetsnummer: String,
		data: ArenaData,
		arenaGjennomforing: ArenaTiltakGjennomforing
	): ArenaDataIdTranslation {
		val id = UUID.randomUUID()

		idTranslationRepository.insert(
			ArenaDataIdTranslation(
				amtId = id,
				arenaTableName = data.arenaTableName,
				arenaId = arenaGjennomforing.TILTAKGJENNOMFORING_ID.toString(),
				ignored = !isSupportedTiltak(arenaGjennomforing.TILTAKSKODE),
				getDigest(arenaGjennomforing.toAmtGjennomforing(tiltakId, id, virksomhetsnummer))
			)
		)

		return idTranslationRepository.get(data.arenaTableName, data.arenaId)
			?: throw IllegalStateException("Translation for id ${data.arenaId} in table ${data.arenaTableName} should exist")

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

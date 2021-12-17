package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaTiltak
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
open class TiltakProcessor(
	private val repository: ArenaDataRepository,
	private val idTranslationRepository: ArenaDataIdTranslationRepository,
	kafkaProducer: KafkaProducerClientImpl<String, String>
) : AbstractArenaProcessor<ArenaTiltak>(
	clazz = ArenaTiltak::class.java,
	kafkaProducer = kafkaProducer
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	override fun handle(data: ArenaData) {
		val arenaTiltak = getMainObject(data)

		if (!isSupportedTiltak(arenaTiltak.TILTAKSKODE)) {
			logger.debug("tiltak med kode ${arenaTiltak.TILTAKSKODE} er ikke støttet og sendes ikke videre")
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak."))
			return
		}

		var idTranslation = idTranslationRepository.get(data.arenaTableName, data.arenaId)

		if (idTranslation != null) {
			val digest = getDigest(arenaTiltak.toAmtTiltak(idTranslation.amtId))

			if (idTranslation.currentHash == digest) {
				logger.info("Tiltak med kode ${arenaTiltak.TILTAKSKODE} sendes ikke videre fordi det allerede er sendt (Samme hash)")
				repository.upsert(data.markAsIgnored("Tiltaket er allerede sendt (samme hash)."))
				return
			}
		} else {
			idTranslation = generateTranslation(data, arenaTiltak)
		}

		val amtData = AmtWrapper(
			type = "TILTAK",
			operation = data.operation,
			before = data.before?.toAmtTiltak(idTranslation.amtId),
			after = data.after?.toAmtTiltak(idTranslation.amtId)

		)

		send(objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsSent())
		logger.info("[Transaction id: ${amtData.transactionId}] [Operation: ${amtData.operation}] Tiltak with id ${idTranslation.amtId} Sent.")
	}

	private fun generateTranslation(data: ArenaData, tiltak: ArenaTiltak): ArenaDataIdTranslation {
		val id = UUID.randomUUID()

		idTranslationRepository.insert(
			ArenaDataIdTranslation(
				amtId = id,
				arenaTableName = data.arenaTableName,
				arenaId = data.arenaId,
				ignored = !isSupportedTiltak(tiltak.TILTAKSKODE),
				getDigest(tiltak.toAmtTiltak(id))
			)
		)

		return idTranslationRepository.get(data.arenaTableName, data.arenaId)
			?: throw IllegalStateException("Translation for id ${data.arenaId} in table ${data.arenaTableName} should exist")
	}

	private fun String.toAmtTiltak(tiltakId: UUID): AmtTiltak {
		return jsonObject(this, ArenaTiltak::class.java)?.toAmtTiltak(tiltakId)
			?: throw IllegalArgumentException("Expected String not to be null")
	}

	private fun ArenaTiltak.toAmtTiltak(tiltakId: UUID): AmtTiltak {
		return AmtTiltak(
			id = tiltakId,
			kode = TILTAKSKODE,
			navn = TILTAKSNAVN
		)
	}


}

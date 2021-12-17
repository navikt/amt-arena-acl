package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.Creation
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

		val id = idTranslationRepository.getAmtId(data.arenaTableName, data.arenaId)
			?: UUID.randomUUID()

		val amtTiltak = arenaTiltak.toAmtTiltak(id)

		if (isIgnored(arenaTiltak)) {
			logger.debug("tiltak med kode ${arenaTiltak.TILTAKSKODE} er ikke støttet og sendes ikke videre")
			getTranslation(data, amtTiltak)
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak."))
			return
		}

		val translation = getTranslation(data, amtTiltak)

		if (translation.first == Creation.EXISTED) {
			val digest = getDigest(amtTiltak)

			if (translation.second.currentHash == digest) {
				logger.info("Tiltak med kode $id sendes ikke videre fordi det allerede er sendt (Samme hash)")
				repository.upsert(data.markAsIgnored("Tiltaket er allerede sendt (samme hash)."))
				return
			}
		}

		val amtData = AmtWrapper(
			type = "TILTAK",
			operation = data.operation,
			before = data.before?.toAmtTiltak(id),
			after = data.after?.toAmtTiltak(id)
		)

		send(objectMapper.writeValueAsString(amtData))
		repository.upsert(data.markAsSent())
		logger.info("[Transaction id: ${amtData.transactionId}] [Operation: ${amtData.operation}] Tiltak with id $id Sent.")
	}

	private fun isIgnored(tiltak: ArenaTiltak): Boolean {
		return !isSupportedTiltak(tiltak.TILTAKSKODE)
	}

	private fun getTranslation(data: ArenaData, tiltak: AmtTiltak): Pair<Creation, ArenaDataIdTranslation> {
		val exists = idTranslationRepository.get(data.arenaTableName, data.arenaId)

		if (exists != null) {
			return Pair(Creation.EXISTED, exists)
		} else {
			idTranslationRepository.insert(
				ArenaDataIdTranslation(
					amtId = tiltak.id,
					arenaTableName = data.arenaTableName,
					arenaId = data.arenaId,
					ignored = !isSupportedTiltak(tiltak.kode),
					getDigest(tiltak)
				)
			)

			val created = idTranslationRepository.get(data.arenaTableName, data.arenaId)
				?: throw IllegalStateException("Translation for id ${data.arenaId} in table ${data.arenaTableName} should exist")

			return Pair(Creation.CREATED, created)
		}
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

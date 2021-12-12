package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaTiltak
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import java.util.*

@Component
open class TiltakProcessor(
	private val repository: ArenaDataRepository,
	private val idTranslationRepository: ArenaDataIdTranslationRepository,
	private val kafkaProducer: KafkaProducerClientImpl<String, String>
) : AbstractArenaProcessor() {

	@Value("\${app.env.amtTopic}")
	lateinit var topic: String

	private val logger = LoggerFactory.getLogger(javaClass)

	override fun handle(data: ArenaData) {
		val arenaTiltak = getMainObject(data)

		val translation = idTranslationRepository.get(data.arenaTableName, data.arenaId)?.let {
			if (it.ignored) {
				logger.debug("tiltak med kode ${arenaTiltak.TILTAKSKODE} er ikke støttet og sendes ikke videre")
				repository.upsert(data.markAsIgnored("Ikke et støttet tiltak."))
				return
			}

			val digest = getDigest(arenaTiltak.toAmtTiltak(it.amtId))

			if (it.currentHash == digest) {
				logger.info("Tiltak med kode ${arenaTiltak.TILTAKSKODE} sendes ikke videre fordi det allerede er sendt (Samme hash)")
				repository.upsert(data.markAsIgnored("Tiltaket er allerede sendt (samme hash)."))
				return
			}

			it
		}
			?: generateTranslation(data, arenaTiltak)

		if (translation.ignored) {
			logger.debug("tiltak med kode ${arenaTiltak.TILTAKSKODE} er ikke støttet og sendes ikke videre")
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak."))
			return
		}

		val amtData = AmtWrapper(
			type = "TILTAK",
			operation = data.operation,
			before = data.before?.toAmtTiltak(translation.amtId),
			after = data.after?.toAmtTiltak(translation.amtId)

		)

		kafkaProducer.sendSync(
			ProducerRecord(
				topic,
				objectMapper.writeValueAsString(amtData)
			)
		)

		repository.upsert(data.markAsSent())
		logger.info("[Transaction id: ${amtData.transactionId}] [Operation: ${amtData.operation}] Tiltak with id ${translation.amtId} Sent.")
	}

	private fun generateTranslation(data: ArenaData, tiltak: ArenaTiltak): ArenaDataIdTranslation {
		val ignored = !isSupportedTiltak(tiltak.TILTAKSKODE)
		val id = UUID.randomUUID()

		idTranslationRepository.insert(
			ArenaDataIdTranslation(
				amtId = id,
				arenaTableName = data.arenaTableName,
				arenaId = data.arenaId,
				ignored,
				getDigest(tiltak.toAmtTiltak(id))
			)
		)

		return idTranslationRepository.get(data.arenaTableName, data.arenaId)
			?: throw IllegalStateException("Translation for id ${data.arenaId} in table ${data.arenaTableName} should exist")
	}

	private fun getDigest(tiltak: AmtTiltak): String {
		return DigestUtils.md5DigestAsHex(objectMapper.writeValueAsString(tiltak).toByteArray())
	}

	private fun getMainObject(data: ArenaData): ArenaTiltak {
		return when (data.operation) {
			AmtOperation.CREATED -> jsonObject(data.after, ArenaTiltak::class.java)
			AmtOperation.MODIFIED -> jsonObject(data.after, ArenaTiltak::class.java)
			AmtOperation.DELETED -> jsonObject(data.before, ArenaTiltak::class.java)
		}
			?: throw IllegalArgumentException("Expected ${data.arenaTableName} id ${data.arenaId} to have before or after correctly set.")
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

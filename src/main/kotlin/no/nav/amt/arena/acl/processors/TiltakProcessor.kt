package no.nav.amt.arena.acl.processors

import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaTiltak
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import java.util.*

@Component
open class TiltakProcessor(
	private val repository: ArenaDataRepository,
	private val idTranslationRepository: ArenaDataIdTranslationRepository,
) : AbstractArenaProcessor() {

	private val logger = LoggerFactory.getLogger(javaClass)

	override fun handle(data: ArenaData) {
		val arenaTiltak = getMainObject(data)

		val translation = idTranslationRepository.get(data.arenaTableName, data.arenaId)
			?: generateTranslation(data, arenaTiltak)

		if (translation.ignored) {
			repository.upsert(data.markAsIgnored("Ikke et støttet tiltak (${arenaTiltak.TILTAKSKODE})"))
			return
		}

		if (translation.currentHash == getDigest(arenaTiltak)) {
			repository.upsert(data.markAsIgnored("Tiltaket er allerede sendt. (samme hash)"))
			return
		}

		val amtData = AmtWrapper(
			type = "TILTAK",
			operation = data.operation,
			before = data.before?.toAmtTiltak(translation.amtId),
			after = data.after?.toAmtTiltak(translation.amtId)

		)

		//TODO Send on Kafka

		repository.upsert(data.markAsIngested())
		logger.info("[Transaction id: ${amtData.transactionId}] [Operation: ${amtData.operation}] Tiltak with id ${translation.amtId} Sent.")
	}

	private fun generateTranslation(data: ArenaData, tiltak: ArenaTiltak): ArenaDataIdTranslation {
		val ignored = !isSupportedTiltak(tiltak.TILTAKSKODE)

		idTranslationRepository.insert(
			ArenaDataIdTranslation(
				amtId = UUID.randomUUID(),
				arenaTableName = data.arenaTableName,
				arenaId = data.arenaId,
				ignored,
				getDigest(tiltak)
			)
		)

		return idTranslationRepository.get(data.arenaTableName, data.arenaId)
			?: throw IllegalStateException("Translation for id ${data.arenaId} in table ${data.arenaTableName} should exist")
	}

	private fun getDigest(tiltak: ArenaTiltak): String {
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

		val arenaTiltak = jsonObject(this, ArenaTiltak::class.java)
			?: throw IllegalArgumentException("Expected String not to be null")

		return AmtTiltak(
			id = tiltakId,
			kode = arenaTiltak.TILTAKSKODE,
			navn = arenaTiltak.TILTAKSNAVN
		)
	}

}

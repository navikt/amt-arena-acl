package no.nav.amt.arena.acl.integration.utils

import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.arena.ArenaTiltak
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import no.nav.amt.arena.acl.utils.TILTAK_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.fail
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class IntegrationTestTiltakUtils(
	private val kafkaProducer: KafkaProducerClientImpl<String, String>,
	private val arenaDataRepository: ArenaDataRepository,
	private val tiltakRepository: TiltakRepository
) {

	private val topic = "tiltak"
	private val logger = LoggerFactory.getLogger(javaClass)

	companion object {
		private const val GENERIC_STRING = "STRING_NOT_SET"
		private const val GENERIC_INT = Int.MIN_VALUE

		val objectMapper = ObjectMapperFactory.get()

		private val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
	}

	fun nyttTiltak(
		position: Int,
		kode: String,
		navn: String
	): TiltakIntegrationTestResult {
		val wrapper = ArenaWrapper(
			table = "SIAMO.TILTAK",
			operation = ArenaOperation.I,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = "$position",
			before = null,
			after = tiltakPayload(kode, navn)
		)

		return sendAndGet(wrapper, kode)
	}

	fun oppdaterTiltak(
		position: Int,
		kode: String,
		gammeltNavn: String,
		nyttNavn: String
	): TiltakIntegrationTestResult {
		val wrapper = ArenaWrapper(
			table = "SIAMO.TILTAK",
			operation = ArenaOperation.U,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = "$position",
			before = tiltakPayload(kode, gammeltNavn),
			after = tiltakPayload(kode, nyttNavn)
		)

		return sendAndGet(wrapper, kode)
	}

	fun slettTiltak(
		position: Int,
		kode: String,
		navn: String
	): TiltakIntegrationTestResult {
		val wrapper = ArenaWrapper(
			table = "SIAMO.TILTAK",
			operation = ArenaOperation.D,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = "$position",
			before = tiltakPayload(kode, navn),
			after = null
		)

		return sendAndGet(wrapper, kode)
	}

	private fun sendAndGet(arenaWrapper: ArenaWrapper, kode: String): TiltakIntegrationTestResult {
		kafkaProducer.send(ProducerRecord(topic, objectMapper.writeValueAsString(arenaWrapper)))

		val data = getArenaData(arenaWrapper.operation.toAmtOperation(), arenaWrapper.operationPosition)
		val storedTiltak = tiltakRepository.getByKode(kode)
			?: fail("Forventet at tiltak med kode $kode ligger i tiltak databasen.")

		return TiltakIntegrationTestResult(
			data,
			storedTiltak
		)
	}


	private fun tiltakPayload(kode: String, navn: String): JsonNode {
		val data = ArenaTiltak(
			TILTAKSKODE = kode,
			TILTAKSNAVN = navn,
			TILTAKSGRUPPEKODE = GENERIC_STRING,
			REG_DATO = GENERIC_STRING,
			REG_USER = GENERIC_STRING,
			MOD_DATO = GENERIC_STRING,
			MOD_USER = GENERIC_STRING,
			DATO_FRA = GENERIC_STRING,
			DATO_TIL = GENERIC_STRING,
			AVSNITT_ID_GENERELT = GENERIC_INT,
			STATUS_BASISYTELSE = GENERIC_STRING,
			ADMINISTRASJONKODE = GENERIC_STRING,
			STATUS_KOPI_TILSAGN = GENERIC_STRING,
			ARKIVNOKKEL = GENERIC_STRING,
			STATUS_ANSKAFFELSE = GENERIC_STRING,
			MAKS_ANT_PLASSER = GENERIC_INT,
			MAKS_ANT_SOKERE = GENERIC_INT,
			STATUS_FAST_ANT_PLASSER = GENERIC_STRING,
			STATUS_SJEKK_ANT_DELTAKERE = GENERIC_STRING,
			STATUS_KALKULATOR = GENERIC_STRING,
			RAMMEAVTALE = GENERIC_STRING,
			OPPLAERINGSGRUPPE = GENERIC_STRING,
			HANDLINGSPLAN = GENERIC_STRING,
			STATUS_SLUTTDATO = GENERIC_STRING,
			MAKS_PERIODE = GENERIC_INT,
			STATUS_MELDEPLIKT = GENERIC_STRING,
			STATUS_VEDTAK = GENERIC_STRING,
			STATUS_IA_AVTALE = GENERIC_STRING,
			STATUS_TILLEGGSSTONADER = GENERIC_STRING,
			STATUS_UTDANNING = GENERIC_STRING,
			AUTOMATISK_TILSAGNSBREV = GENERIC_STRING,
			STATUS_BEGRUNNELSE_INNSOKT = GENERIC_STRING,
			STATUS_HENVISNING_BREV = GENERIC_STRING,
			STATUS_KOPIBREV = GENERIC_STRING
		)

		return objectMapper.readTree(objectMapper.writeValueAsString(data))
	}

	private fun ArenaOperation.toAmtOperation(): AmtOperation {
		return when (this) {
			ArenaOperation.I -> AmtOperation.CREATED
			ArenaOperation.U -> AmtOperation.MODIFIED
			ArenaOperation.D -> AmtOperation.DELETED
		}
	}

	private fun getArenaData(operation: AmtOperation, position: String): ArenaData {
		var attempts = 0
		while (attempts < 10) {
			val data = arenaDataRepository.getAll()
			data.forEach { entry ->
				if (entry.arenaTableName == TILTAK_TABLE_NAME && entry.operation == operation && entry.operationPosition == position) {
					logger.info("Fant Arena data i tabell $TILTAK_TABLE_NAME med operasjon $operation og posisjon $position etter $attempts forsøk.")
					return entry
				}
			}

			Thread.sleep(500)
			attempts++
		}

		fail("Could not find Arena data in table $TILTAK_TABLE_NAME with operation $operation and position $position")
	}

}

data class TiltakIntegrationTestResult(
	val arenaData: ArenaData,
	val tiltak: AmtTiltak
)

package no.nav.amt.arena.acl.integration.utils

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.IngestStatus
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
import java.util.*

class TiltakIntegrationTestHandler(
	private val kafkaProducer: KafkaProducerClientImpl<String, String>,
	private val arenaDataRepository: ArenaDataRepository,
	private val tiltakRepository: TiltakRepository
) {

	private val topic = "tiltak"
	private val logger = LoggerFactory.getLogger(javaClass)

	var currentInput: TiltakIntegrationTestInput? = null
	var currentResult: TiltakIntegrationTestResult? = null

	companion object {
		private const val GENERIC_STRING = "STRING_NOT_SET"
		private const val GENERIC_INT = Int.MIN_VALUE

		val objectMapper = ObjectMapperFactory.get()

		private val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
	}

	fun nyttTiltak(
		input: TiltakIntegrationTestInput
	): TiltakIntegrationTestHandler {
		val wrapper = ArenaWrapper(
			table = TILTAK_TABLE_NAME,
			operation = ArenaOperation.I,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = input.position,
			before = null,
			after = tiltakPayload(input.kode, input.navn)
		)

		currentInput = input
		currentResult = getResult(wrapper, input.kode)

		return this
	}

	fun oppdaterTiltak(
		position: String,
		nyttNavn: String
	): TiltakIntegrationTestHandler {
		val i = currentInput ?: throw IllegalStateException("Kan ikke oppdatere tiltak uten først å opprette det")

		val wrapper = ArenaWrapper(
			table = TILTAK_TABLE_NAME,
			operation = ArenaOperation.U,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = position,
			before = tiltakPayload(i.kode, i.navn),
			after = tiltakPayload(i.kode, nyttNavn)
		)

		currentInput = i.copy(position = position, navn = nyttNavn)
		currentResult = getResult(wrapper, i.kode)

		return this
	}

	fun slettTiltak(
		position: String
	): TiltakIntegrationTestHandler {
		val i = currentInput ?: throw IllegalStateException("Kan ikke slette tiltak uten først å opprette det")

		val wrapper = ArenaWrapper(
			table = TILTAK_TABLE_NAME,
			operation = ArenaOperation.D,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = position,
			before = tiltakPayload(i.kode, i.navn),
			after = null
		)

		currentInput = i.copy(position = position)
		currentResult = getResult(wrapper, i.kode)

		return this
	}

	fun arenaData(check: (data: ArenaData, currentInput: TiltakIntegrationTestInput) -> Unit): TiltakIntegrationTestHandler {
		currentResult shouldNotBe null
		currentResult!!.arenaData shouldNotBe null
		currentInput shouldNotBe null
		check.invoke(currentResult!!.arenaData, currentInput!!)

		return this
	}

	fun tiltak(check:(data: AmtTiltak, currentInput: TiltakIntegrationTestInput) -> Unit): TiltakIntegrationTestHandler {
		currentResult shouldNotBe null
		currentResult!!.tiltak shouldNotBe null
		currentInput shouldNotBe null
		check.invoke(currentResult!!.tiltak, currentInput!!)

		return this
	}

	private fun getResult(arenaWrapper: ArenaWrapper, kode: String): TiltakIntegrationTestResult {
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

data class TiltakIntegrationTestInput(
	val position: String,
	val kode: String = "INDOPPFAG",
	val navn: String = UUID.randomUUID().toString()
)

data class TiltakIntegrationTestResult(
	val arenaData: ArenaData,
	val tiltak: AmtTiltak
)

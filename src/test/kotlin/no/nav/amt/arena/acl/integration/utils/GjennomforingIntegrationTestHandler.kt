package no.nav.amt.arena.acl.integration.utils

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.ArenaDataIdTranslation
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.domain.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.amt.AmtWrapper
import no.nav.amt.arena.acl.domain.arena.ArenaOperation
import no.nav.amt.arena.acl.domain.arena.ArenaTiltakGjennomforing
import no.nav.amt.arena.acl.domain.arena.ArenaWrapper
import no.nav.amt.arena.acl.integration.kafka.KafkaAmtIntegrationConsumer
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import no.nav.amt.arena.acl.utils.TILTAKGJENNOMFORING_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.fail
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class GjennomforingIntegrationTestHandler(
	private val kafkaProducer: KafkaProducerClientImpl<String, String>,
	private val arenaDataRepository: ArenaDataRepository,
	private val translationRepository: ArenaDataIdTranslationRepository
) {

	private val topic = "gjennomforing"
	private val logger = LoggerFactory.getLogger(javaClass)

	var currentInput: GjennomforingIntegrationTestInput? = null
	var currentResult: GjennomforingIntegrationTestResult? = null

	val outputMessages = mutableListOf<AmtWrapper<AmtGjennomforing>>()

	init {
		setupMessageReader()
	}

	private fun setupMessageReader() {
		KafkaAmtIntegrationConsumer.subscribeGjennomforing { outputMessages.add(it) }
	}

	companion object {
		private const val GENERIC_STRING = "STRING_NOT_SET"
		private const val GENERIC_INT = Int.MIN_VALUE
		private const val GENERIC_LONG = Long.MIN_VALUE
		private const val GENERIC_FLOAT = Float.MIN_VALUE

		private val opTsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
		private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

		val objectMapper = ObjectMapperFactory.get()
	}

	fun nyGjennomforing(input: GjennomforingIntegrationTestInput): GjennomforingIntegrationTestHandler {
		val wrapper = ArenaWrapper(
			table = TILTAKGJENNOMFORING_TABLE_NAME,
			operation = ArenaOperation.I,
			operationTimestampString = LocalDateTime.now().format(opTsFormatter),
			operationPosition = input.position,
			before = null,
			after = gjennomforingPayload(input)
		)

		currentInput = input
		currentResult = getResult(wrapper)

		return this
	}

	fun shouldHaveIngestStatus(expected: IngestStatus): GjennomforingIntegrationTestHandler {
		currentResult shouldNotBe null
		currentResult!!.arenaData.ingestStatus shouldBe expected
		return this
	}

	fun outputShouldHaveOperation(expected: AmtOperation): GjennomforingIntegrationTestHandler {
		currentResult shouldNotBe null
		currentResult!!.output shouldNotBe null
		currentResult!!.output!!.operation shouldBe expected
		return this
	}

	fun validateOutput(): GjennomforingIntegrationTestHandler {
		currentResult shouldNotBe null
		currentResult!!.output shouldNotBe null
		currentResult!!.translation shouldNotBe null

		val output = currentResult!!.output!!.payload!!
		output.id shouldBe currentResult!!.translation!!.amtId
		output.navn shouldBe currentInput!!.navn
		output.tiltak.kode shouldBe currentInput!!.tiltakKode
		return this
	}

	private fun gjennomforingPayload(input: GjennomforingIntegrationTestInput): JsonNode {
		val data = ArenaTiltakGjennomforing(
			TILTAKGJENNOMFORING_ID = input.gjennomforingId,
			SAK_ID = GENERIC_LONG,
			TILTAKSKODE = input.tiltakKode,
			ANTALL_DELTAKERE = GENERIC_INT,
			ANTALL_VARIGHET = GENERIC_INT,
			DATO_FRA = dateFormatter.format(input.startDato.atStartOfDay()),
			DATO_TIL = dateFormatter.format(input.sluttDato.atStartOfDay()),
			FAGPLANKODE = GENERIC_STRING,
			MAALEENHET_VARIGHET = GENERIC_STRING,
			TEKST_FAGBESKRIVELSE = GENERIC_STRING,
			TEKST_KURSSTED = GENERIC_STRING,
			TEKST_MAALGRUPPE = GENERIC_STRING,
			STATUS_TREVERDIKODE_INNSOKNING = GENERIC_STRING,
			REG_DATO = dateFormatter.format(input.registrertDato),
			REG_USER = GENERIC_STRING,
			MOD_DATO = GENERIC_STRING,
			MOD_USER = GENERIC_STRING,
			LOKALTNAVN = input.navn,
			TILTAKSTATUSKODE = input.tiltakStatusKode,
			PROSENT_DELTID = GENERIC_FLOAT,
			KOMMENTAR = GENERIC_STRING,
			ARBGIV_ID_ARRANGOR = input.arbeidsgiverIdArrangor,
			PROFILELEMENT_ID_GEOGRAFI = GENERIC_STRING,
			KLOKKETID_FREMMOTE = null,
			DATO_FREMMOTE = dateFormatter.format(LocalDateTime.now()),
			BEGRUNNELSE_STATUS = GENERIC_STRING,
			AVTALE_ID = GENERIC_LONG,
			AKTIVITET_ID = GENERIC_LONG,
			DATO_INNSOKNINGSTART = GENERIC_STRING,
			GML_FRA_DATO = GENERIC_STRING,
			GML_TIL_DATO = GENERIC_STRING,
			AETAT_FREMMOTEREG = GENERIC_STRING,
			AETAT_KONTERINGSSTED = GENERIC_STRING,
			OPPLAERINGNIVAAKODE = GENERIC_STRING,
			TILTAKGJENNOMFORING_ID_REL = GENERIC_STRING,
			VURDERING_GJENNOMFORING = GENERIC_STRING,
			PROFILELEMENT_ID_OPPL_TILTAK = GENERIC_STRING,
			DATO_OPPFOLGING_OK = GENERIC_STRING,
			PARTISJON = GENERIC_LONG,
			MAALFORM_KRAVBREV = GENERIC_STRING
		)

		return objectMapper.readTree(objectMapper.writeValueAsString(data))
	}

	private fun getResult(arenaWrapper: ArenaWrapper): GjennomforingIntegrationTestResult {
		kafkaProducer.send(
			ProducerRecord(
				topic,
				objectMapper.writeValueAsString(arenaWrapper)
			)
		)

		val arenaData = getArenaData(arenaWrapper.operation.toAmtOperation(), arenaWrapper.operationPosition)
		val translation = getTranslation(arenaData.arenaId)
		val message = if (translation != null) getOutputMessage(translation.amtId) else null

		return GjennomforingIntegrationTestResult(
			arenaData,
			translation,
			message
		)
	}

	private fun ArenaOperation.toAmtOperation(): AmtOperation {
		return when (this) {
			ArenaOperation.I -> AmtOperation.CREATED
			ArenaOperation.U -> AmtOperation.MODIFIED
			ArenaOperation.D -> AmtOperation.DELETED
		}
	}


	private fun getArenaData(operation: AmtOperation, position: String): ArenaData {
		val tableName = TILTAKGJENNOMFORING_TABLE_NAME
		var attempts = 0
		while (attempts < 10) {
			val data = arenaDataRepository.getAll()
			data.forEach { entry ->
				if (entry.arenaTableName == tableName && entry.operation == operation && entry.operationPosition == position) {
					logger.info("Fant Arena data i tabell $tableName med operasjon $operation og posisjon $position etter $attempts forsøk.")
					return entry
				}
			}

			Thread.sleep(500)
			attempts++
		}

		fail("Could not find Arena data in table $tableName with operation $operation and position $position")
	}

	private fun getTranslation(arenaId: String): ArenaDataIdTranslation? {
		val tableName = TILTAKGJENNOMFORING_TABLE_NAME
		var attempts = 0
		while (attempts < 5) {
			val data = translationRepository.get(tableName, arenaId)

			if (data != null) {
				return data
			}

			Thread.sleep(250)
			attempts++
		}

		return null
	}

	private fun getOutputMessage(id: UUID): AmtWrapper<AmtGjennomforing>? {
		var attempts = 0
		while (attempts < 5) {
			val data = outputMessages.firstOrNull { it.payload != null && (it.payload as AmtGjennomforing).id == id }

			if (data != null) {
				return data
			}

			Thread.sleep(250)
			attempts++
		}

		return null
	}

}

data class GjennomforingIntegrationTestInput(
	val position: String,
	val tiltakKode: String = "INDOPPFAG",
	val gjennomforingId: Long,
	val arbeidsgiverIdArrangor: Long = 0,
	val navn: String = UUID.randomUUID().toString(),
	val startDato: LocalDate = LocalDate.now().minusDays(7),
	val sluttDato: LocalDate = LocalDate.now().plusDays(7),
	val registrertDato: LocalDateTime = LocalDateTime.now().minusDays(14).truncatedTo(ChronoUnit.HOURS),
	val tiltakStatusKode: String = "GJENNOMFOR"
)

data class GjennomforingIntegrationTestResult(
	val arenaData: ArenaData,
	val translation: ArenaDataIdTranslation?,
	val output: AmtWrapper<AmtGjennomforing>?
)

package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforing
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.repositories.*
import no.nav.amt.arena.acl.services.*
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils.fromJsonString
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TiltakGjennomforingProcessorTest {
	private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
	private lateinit var repository: ArenaDataRepository
	private lateinit var translationRepository: ArenaDataIdTranslationRepository
	private lateinit var ordsClient: ArenaOrdsProxyClient
	private lateinit var kafkaProducerService: KafkaProducerService
	private lateinit var gjennomforingProcessor: GjennomforingProcessor
	private lateinit var gjennomforingRepository: GjennomforingRepository
	private lateinit var gjennomforingService: GjennomforingService

	val dataSource = SingletonPostgresContainer.getDataSource()
	var tiltakKode = "INDOPPFAG"
	var ARBGIV_ID_ARRANGOR = "661733"
	var sakId = Random().nextLong()
	var arenaGjennomforingId = 3728063

	@BeforeAll
	fun beforeAll() {
		jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		repository = ArenaDataRepository(jdbcTemplate)
		translationRepository = ArenaDataIdTranslationRepository(jdbcTemplate)
		ordsClient = mock(ArenaOrdsProxyClient::class.java)
		kafkaProducerService = mock(KafkaProducerService::class.java)
		gjennomforingRepository = GjennomforingRepository(jdbcTemplate)
		gjennomforingService = GjennomforingService(
			gjennomforingRepository
		)

		gjennomforingProcessor = GjennomforingProcessor(
			repository,
			gjennomforingService
		)
		`when`(ordsClient.hentVirksomhetsnummer(ARBGIV_ID_ARRANGOR)).thenReturn("123")
	}

	@BeforeEach
	fun beforeEach() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@Test
	fun `handleEntry() - Gyldig gjennomføring - inserter i korrekte tabeller`() {
		val opPos = "1"
		val arenaGjennomforing = fromJsonString<ArenaGjennomforing>(arenaGjennomforingJson)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforing = arenaGjennomforing,
		)

		gjennomforingProcessor.handleArenaMessage(kafkaMessage)

		val gjennomforing = gjennomforingService.get(arenaGjennomforingId.toString())
		gjennomforing shouldNotBe null
		gjennomforing!!.isValid shouldBe true
		gjennomforing.isSupported shouldBe true

		repository.get(
			kafkaMessage.arenaTableName,
			AmtOperation.CREATED,
			opPos
		)!!.ingestStatus shouldBe IngestStatus.HANDLED

	}

	@Test
	fun `handleEntry() - ugyldig gjennomføring - skal markeres som invalid`() {
		val opPos = "2"

		val arenaGjennomforing = fromJsonString<ArenaGjennomforing>(arenaGjennomforingUgyldigJson)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforing = arenaGjennomforing,
		)

		gjennomforingProcessor.handleArenaMessage(kafkaMessage)
		val gjennomforing = gjennomforingService.get(arenaGjennomforingId.toString())

		gjennomforing shouldNotBe null
		gjennomforing!!.isValid shouldBe false
	}

	@Test
	fun `handleEntry() - ugyldig gjennomføring blir gyldig - skal markeres som valid`() {
		val opPos = "2"

		val arenaGjennomforingUgyldig = fromJsonString<ArenaGjennomforing>(arenaGjennomforingUgyldigJson)
		val arenaGjennomforingGyldig = fromJsonString<ArenaGjennomforing>(arenaGjennomforingJson)


		val kafkaMessageUgyldig = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforing = arenaGjennomforingUgyldig,
		)

		gjennomforingProcessor.handleArenaMessage(kafkaMessageUgyldig)

		gjennomforingService.get(arenaGjennomforingId.toString())!!.isValid shouldBe false

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforing = arenaGjennomforingGyldig,
		)
		gjennomforingProcessor.handleArenaMessage(kafkaMessage)

		gjennomforingService.get(arenaGjennomforingId.toString())!!.isValid shouldBe true

	}


	@Test
	fun `handleEntry() - tiltaktype er ikke støttet - skal lagres i riktige tabeller`() {
		val opPos = "2"

		val arenaGjennomforing = fromJsonString<ArenaGjennomforing>(arenaGjennomforingUkjentTypeJson)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforing = arenaGjennomforing,
		)
		gjennomforingProcessor.handleArenaMessage(kafkaMessage)
		gjennomforingService.get(arenaGjennomforingId.toString())!!.isValid shouldBe true

		repository.get(
			kafkaMessage.arenaTableName,
			AmtOperation.CREATED,
			opPos
		) shouldBe null

	}

	@Test
	fun `handleEntry() - operation type delete - skal sendes videre`() {
		val opPos = "11223344"

		val arenaGjennomforing = fromJsonString<ArenaGjennomforing>(arenaGjennomforingJson)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			operationType = AmtOperation.DELETED,
			arenaGjennomforing = arenaGjennomforing,
		)

		gjennomforingProcessor.handleArenaMessage(kafkaMessage)

		gjennomforingService.get(arenaGjennomforingId.toString()) shouldNotBe null

		repository.get(
			ARENA_GJENNOMFORING_TABLE_NAME,
			AmtOperation.DELETED,
			opPos
		)!!.ingestStatus shouldBe IngestStatus.HANDLED
	}

	private fun createArenaGjennomforingKafkaMessage(
		operationPosition: String = "1",
		operationType: AmtOperation = AmtOperation.CREATED,
		operationTimestamp: LocalDateTime = LocalDateTime.now(),
		arenaGjennomforing: ArenaGjennomforing,
	): ArenaGjennomforingKafkaMessage {
		return ArenaGjennomforingKafkaMessage(
			arenaTableName = ARENA_GJENNOMFORING_TABLE_NAME,
			operationType = operationType,
			operationTimestamp = operationTimestamp,
			operationPosition = operationPosition,
			before = if (listOf(
					AmtOperation.MODIFIED,
					AmtOperation.DELETED
				).contains(operationType)
			) arenaGjennomforing else null,
			after = if (listOf(
					AmtOperation.CREATED,
					AmtOperation.MODIFIED
				).contains(operationType)
			) arenaGjennomforing else null,
		)
	}

	private val arenaGjennomforingUgyldigJson = """
		{
		  "TILTAKGJENNOMFORING_ID": $arenaGjennomforingId,
		  "SAK_ID": 13467550,
		  "TILTAKSKODE": "$tiltakKode",
		  "ANTALL_DELTAKERE": 70,
		  "ANTALL_VARIGHET": null,
		  "DATO_FRA": "2021-12-01 00:00:00",
		  "DATO_TIL": "2023-12-31 00:00:00",
		  "FAGPLANKODE": null,
		  "MAALEENHET_VARIGHET": null,
		  "TEKST_FAGBESKRIVELSE": "asdfds",
		  "TEKST_KURSSTED": null,
		  "TEKST_MAALGRUPPE": "sdfsdf",
		  "STATUS_TREVERDIKODE_INNSOKNING": "J",
		  "REG_DATO": "2021-12-01 10:54:36",
		  "REG_USER": "BRUKER123",
		  "MOD_DATO": "2021-12-01 10:57:19",
		  "MOD_USER": "BRUKER123",
		  "LOKALTNAVN": "testtiltak for komet - oppfølgingstiltak",
		  "TILTAKSTATUSKODE": "GJENNOMFOR",
		  "PROSENT_DELTID": 100,
		  "KOMMENTAR": null,
		  "ARBGIV_ID_ARRANGOR": null,
		  "PROFILELEMENT_ID_GEOGRAFI": null,
		  "KLOKKETID_FREMMOTE": null,
		  "DATO_FREMMOTE": null,
		  "BEGRUNNELSE_STATUS": null,
		  "AVTALE_ID": 315487,
		  "AKTIVITET_ID": 133910244,
		  "DATO_INNSOKNINGSTART": null,
		  "GML_FRA_DATO": null,
		  "GML_TIL_DATO": null,
		  "AETAT_FREMMOTEREG": "0111",
		  "AETAT_KONTERINGSSTED": "0111",
		  "OPPLAERINGNIVAAKODE": null,
		  "TILTAKGJENNOMFORING_ID_REL": null,
		  "VURDERING_GJENNOMFORING": null,
		  "PROFILELEMENT_ID_OPPL_TILTAK": null,
		  "DATO_OPPFOLGING_OK": null,
		  "PARTISJON": null,
		  "MAALFORM_KRAVBREV": "NO"
		}
	""".trimIndent()

	private val arenaGjennomforingUkjentTypeJson = """
		{
		  "TILTAKGJENNOMFORING_ID": $arenaGjennomforingId,
		  "SAK_ID": 13467550,
		  "TILTAKSKODE": "ENNYTYPETILTAK",
		  "ANTALL_DELTAKERE": 70,
		  "ANTALL_VARIGHET": null,
		  "DATO_FRA": "2021-12-01 00:00:00",
		  "DATO_TIL": "2023-12-31 00:00:00",
		  "FAGPLANKODE": null,
		  "MAALEENHET_VARIGHET": null,
		  "TEKST_FAGBESKRIVELSE": "asdfds",
		  "TEKST_KURSSTED": null,
		  "TEKST_MAALGRUPPE": "sdfsdf",
		  "STATUS_TREVERDIKODE_INNSOKNING": "J",
		  "REG_DATO": "2021-12-01 10:54:36",
		  "REG_USER": "BRUKER123",
		  "MOD_DATO": "2021-12-01 10:57:19",
		  "MOD_USER": "BRUKER123",
		  "LOKALTNAVN": "testtiltak for komet - oppfølgingstiltak",
		  "TILTAKSTATUSKODE": "GJENNOMFOR",
		  "PROSENT_DELTID": 100,
		  "KOMMENTAR": null,
		  "ARBGIV_ID_ARRANGOR": $ARBGIV_ID_ARRANGOR,
		  "PROFILELEMENT_ID_GEOGRAFI": null,
		  "KLOKKETID_FREMMOTE": null,
		  "DATO_FREMMOTE": null,
		  "BEGRUNNELSE_STATUS": null,
		  "AVTALE_ID": 315487,
		  "AKTIVITET_ID": 133910244,
		  "DATO_INNSOKNINGSTART": null,
		  "GML_FRA_DATO": null,
		  "GML_TIL_DATO": null,
		  "AETAT_FREMMOTEREG": "0111",
		  "AETAT_KONTERINGSSTED": "0111",
		  "OPPLAERINGNIVAAKODE": null,
		  "TILTAKGJENNOMFORING_ID_REL": null,
		  "VURDERING_GJENNOMFORING": null,
		  "PROFILELEMENT_ID_OPPL_TILTAK": null,
		  "DATO_OPPFOLGING_OK": null,
		  "PARTISJON": null,
		  "MAALFORM_KRAVBREV": "NO"
		}
	""".trimIndent()

	private val arenaGjennomforingJson = """
		{
		  "TILTAKGJENNOMFORING_ID": $arenaGjennomforingId,
		  "SAK_ID": $sakId,
		  "TILTAKSKODE": "$tiltakKode",
		  "ANTALL_DELTAKERE": 70,
		  "ANTALL_VARIGHET": null,
		  "DATO_FRA": "2021-12-01 00:00:00",
		  "DATO_TIL": "2023-12-31 00:00:00",
		  "FAGPLANKODE": null,
		  "MAALEENHET_VARIGHET": null,
		  "TEKST_FAGBESKRIVELSE": "asdfds",
		  "TEKST_KURSSTED": null,
		  "TEKST_MAALGRUPPE": "sdfsdf",
		  "STATUS_TREVERDIKODE_INNSOKNING": "J",
		  "REG_DATO": "2021-12-01 10:54:36",
		  "REG_USER": "BRUKER123",
		  "MOD_DATO": "2021-12-01 10:57:19",
		  "MOD_USER": "BRUKER123",
		  "LOKALTNAVN": "testtiltak for komet - oppfølgingstiltak",
		  "TILTAKSTATUSKODE": "GJENNOMFOR",
		  "PROSENT_DELTID": 100,
		  "KOMMENTAR": null,
		  "ARBGIV_ID_ARRANGOR": $ARBGIV_ID_ARRANGOR,
		  "PROFILELEMENT_ID_GEOGRAFI": null,
		  "KLOKKETID_FREMMOTE": null,
		  "DATO_FREMMOTE": null,
		  "BEGRUNNELSE_STATUS": null,
		  "AVTALE_ID": 315487,
		  "AKTIVITET_ID": 133910244,
		  "DATO_INNSOKNINGSTART": null,
		  "GML_FRA_DATO": null,
		  "GML_TIL_DATO": null,
		  "AETAT_FREMMOTEREG": "0111",
		  "AETAT_KONTERINGSSTED": "0111",
		  "OPPLAERINGNIVAAKODE": null,
		  "TILTAKGJENNOMFORING_ID_REL": null,
		  "VURDERING_GJENNOMFORING": null,
		  "PROFILELEMENT_ID_OPPL_TILTAK": null,
		  "DATO_OPPFOLGING_OK": null,
		  "PARTISJON": null,
		  "MAALFORM_KRAVBREV": "NO"
		}
	""".trimIndent()
}

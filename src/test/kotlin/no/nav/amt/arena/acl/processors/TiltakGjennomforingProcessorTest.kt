package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.amt.AmtTiltak
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforing
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaGjennomforingKafkaMessage
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.ArenaGjennomforingRepository
import no.nav.amt.arena.acl.repositories.ArenaSakRepository
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.services.TiltakService
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TiltakGjennomforingProcessorTest {
	private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
	private lateinit var repository: ArenaDataRepository
	private lateinit var translationRepository: ArenaDataIdTranslationRepository
	private lateinit var tiltakService: TiltakService
	private lateinit var ordsClient: ArenaOrdsProxyClient
	private lateinit var kafkaProducerService: KafkaProducerService
	private lateinit var gjennomforingProcessor: GjennomforingProcessor

	var mapper = ObjectMapperFactory.get()
	val dataSource = SingletonPostgresContainer.getDataSource()
	var tiltakKode = "INDOPPFAG"
	var ukjentTiltakType = "UKJENTTILTAK"
	var ARBGIV_ID_ARRANGOR = "661733"

	@BeforeAll
	fun beforeAll() {
		jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		repository = ArenaDataRepository(jdbcTemplate)
		translationRepository = ArenaDataIdTranslationRepository(jdbcTemplate)
		tiltakService = mock(TiltakService::class.java)
		ordsClient = mock(ArenaOrdsProxyClient::class.java)
		kafkaProducerService = mock(KafkaProducerService::class.java)

		gjennomforingProcessor = GjennomforingProcessor(
			repository,
			ArenaSakRepository(jdbcTemplate),
			ArenaGjennomforingRepository(jdbcTemplate),
			ArenaDataIdTranslationService(translationRepository),
			tiltakService,
			ordsClient,
			kafkaProducerService
		)

		`when`(this.tiltakService.getByKode(tiltakKode)).thenReturn(AmtTiltak(UUID.randomUUID(), kode=tiltakKode, navn="Oppfølging"))
		`when`(ordsClient.hentVirksomhetsnummer(ARBGIV_ID_ARRANGOR)).thenReturn("123")
	}

	@BeforeEach
	fun beforeEach() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@Test
	fun `handleEntry() - Gyldig gjennomføring - inserter i korrekte tabeller`() {
		val opPos = "1"

		val arenaGjennomforing = mapper.readValue(arenaGjennomforingJson, ArenaGjennomforing::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforing = arenaGjennomforing,
		)

		gjennomforingProcessor.handleArenaMessage(kafkaMessage)

		val translationData = translationRepository.get(kafkaMessage.arenaTableName, "3728063")
		translationData!!.arenaId shouldBe "3728063"
		translationData.ignored shouldBe false

		repository.get(kafkaMessage.arenaTableName, AmtOperation.CREATED, opPos).ingestStatus shouldBe IngestStatus.HANDLED

	}

	@Test
	fun `handleEntry() - ugyldig gjennomføring - skal kaste ValidationException`() {
		val opPos = "2"

		val arenaGjennomforing = mapper.readValue(arenaGjennomforingUgyldigJson, ArenaGjennomforing::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforing = arenaGjennomforing,
		)

		shouldThrowExactly<ValidationException> {
			gjennomforingProcessor.handleArenaMessage(kafkaMessage)
		}
	}

	@Test
	fun `handleEntry() - tiltaktype er ikke oppfølging - skal kaste IgnoredException`() {
		val opPos = "2"

		val arenaGjennomforing = mapper.readValue(arenaGjennomforingUkjentTypeJson, ArenaGjennomforing::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			arenaGjennomforing = arenaGjennomforing,
		)

		`when`(tiltakService.getByKode(ukjentTiltakType)).thenReturn(AmtTiltak(UUID.randomUUID(), kode=tiltakKode, navn="Oppfølging"))

		shouldThrowExactly<IgnoredException> {
			gjennomforingProcessor.handleArenaMessage(kafkaMessage)
		}
	}

	@Test
	fun `handleEntry() - operation type delete - skal sendes videre`() {
		val opPos = "11223344"

		val arenaGjennomforing = mapper.readValue(arenaGjennomforingJson, ArenaGjennomforing::class.java)

		val kafkaMessage = createArenaGjennomforingKafkaMessage(
			operationPosition = opPos,
			operationType = AmtOperation.DELETED,
			arenaGjennomforing = arenaGjennomforing,
		)

		gjennomforingProcessor.handleArenaMessage(kafkaMessage)

		val translationData = translationRepository.get(ARENA_GJENNOMFORING_TABLE_NAME, "3728063")
		translationData shouldNotBe null

		repository.get(ARENA_GJENNOMFORING_TABLE_NAME, AmtOperation.DELETED, opPos).ingestStatus shouldBe IngestStatus.HANDLED
	}

	private fun createArenaGjennomforingKafkaMessage(
		operationPosition: String = "1",
		operationType: AmtOperation = AmtOperation.CREATED,
		operationTimestamp: LocalDateTime = LocalDateTime.now(),
		arenaGjennomforing: ArenaGjennomforing,
	): ArenaGjennomforingKafkaMessage {
		return ArenaGjennomforingKafkaMessage(
			arenaTableName =  ARENA_GJENNOMFORING_TABLE_NAME,
			operationType = operationType,
			operationTimestamp = operationTimestamp,
			operationPosition =  operationPosition,
			before = if (listOf(AmtOperation.MODIFIED, AmtOperation.DELETED).contains(operationType)) arenaGjennomforing else null,
			after =  if (listOf(AmtOperation.CREATED, AmtOperation.MODIFIED).contains(operationType)) arenaGjennomforing else null,
		)
	}

	private val arenaGjennomforingUgyldigJson = """
		{
		  "TILTAKGJENNOMFORING_ID": 830743204,
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
		  "TILTAKGJENNOMFORING_ID": 7843295,
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
		  "TILTAKGJENNOMFORING_ID": 3728063,
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

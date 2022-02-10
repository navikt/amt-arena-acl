package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import no.nav.amt.arena.acl.domain.amt.AmtTiltak
import no.nav.amt.arena.acl.repositories.ArenaDataIdTranslationRepository
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.utils.TILTAKGJENNOMFORING_TABLE_NAME
import no.nav.common.kafka.producer.KafkaProducerClientImpl
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.*
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TiltakGjennomforingProcessorTest {
	private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
	private lateinit var repository: ArenaDataRepository
	private lateinit var translationRepository: ArenaDataIdTranslationRepository
	private lateinit var tiltakRepository: TiltakRepository
	private lateinit var ordsClient: ArenaOrdsProxyClient
	private lateinit var kafkaProducer: KafkaProducerClientImpl<String, String>
	private lateinit var gjennomforingProcessor: TiltakGjennomforingProcessor
	private var meterRegistry = SimpleMeterRegistry()
	var mapper: ObjectMapper = ObjectMapper()
	val dataSource = SingletonPostgresContainer.getDataSource()
	var tiltakKode = "INDOPPFAG"
	var ukjentTiltakType = "UKJENTTILTAK"
	var ARBGIV_ID_ARRANGOR = "661733"

	@BeforeAll
	fun beforeAll() {
		jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
		repository = ArenaDataRepository(jdbcTemplate)
		translationRepository = ArenaDataIdTranslationRepository(jdbcTemplate)
		tiltakRepository = mock(TiltakRepository::class.java)
		ordsClient = mock(ArenaOrdsProxyClient::class.java)
		kafkaProducer = mock(KafkaProducerClientImpl::class.java) as KafkaProducerClientImpl<String, String>
		gjennomforingProcessor = TiltakGjennomforingProcessor(repository, translationRepository, tiltakRepository, ordsClient, meterRegistry, kafkaProducer)

		`when`(tiltakRepository.getByKode(tiltakKode)).thenReturn(AmtTiltak(UUID.randomUUID(), kode=tiltakKode, navn="Oppfølging"))
		`when`(ordsClient.hentVirksomhetsnummer(ARBGIV_ID_ARRANGOR)).thenReturn("123")
		gjennomforingProcessor.topic = "Tralllaal"
	}

	@BeforeEach
	fun beforeEach() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@Test
	fun `handleEntry() - Gyldig gjennomføring - inserter i korrekte tabeller`() {
		val arenaId = "123"
		val opPos = "1"

		val arenaData = ArenaData(
			arenaTableName =  TILTAKGJENNOMFORING_TABLE_NAME,
			arenaId =  arenaId,
			operation =  AmtOperation.CREATED,
			operationPosition =  opPos,
			operationTimestamp =  LocalDateTime.now(),
			after =  arenaGjennomforingJson,
		)

		gjennomforingProcessor.handle(arenaData)
		val translationData = translationRepository.get(arenaData.arenaTableName, arenaData.arenaId)
		translationData!!.arenaId shouldBe arenaId
		translationData.ignored shouldBe false

		repository.get(arenaData.arenaTableName, AmtOperation.CREATED, opPos).ingestStatus shouldBe IngestStatus.HANDLED

	}

	@Test
	fun `handleEntry() - ugyldig gjennomføring - legges til i ignore liste`() {
		val arenaId = "321"
		val opPos = "2"

		val arenaData = ArenaData(
			arenaTableName =  TILTAKGJENNOMFORING_TABLE_NAME,
			arenaId =  arenaId,
			operation =  AmtOperation.CREATED,
			operationPosition =  opPos,
			operationTimestamp =  LocalDateTime.now(),
			after =  arenaGjennomforingUgyldigJson,
		)

		gjennomforingProcessor.handle(arenaData)
		val translationData = translationRepository.get(arenaData.arenaTableName, arenaData.arenaId)
		translationData!!.arenaId shouldBe arenaId
		translationData.ignored shouldBe true

		repository.get(arenaData.arenaTableName, AmtOperation.CREATED, opPos).ingestStatus shouldBe IngestStatus.IGNORED

	}

	@Test
	fun `handleEntry() - tiltaktype er ikke oppfølging - legges til i ignore liste`() {
		val arenaId = "321"
		val opPos = "2"

		val arenaData = ArenaData(
			arenaTableName =  TILTAKGJENNOMFORING_TABLE_NAME,
			arenaId =  arenaId,
			operation =  AmtOperation.CREATED,
			operationPosition =  opPos,
			operationTimestamp =  LocalDateTime.now(),
			after =  arenaGjennomforingUkjentTypeJson,
		)

		`when`(tiltakRepository.getByKode(ukjentTiltakType)).thenReturn(AmtTiltak(UUID.randomUUID(), kode=tiltakKode, navn="Oppfølging"))

		gjennomforingProcessor.handle(arenaData)
		val translationData = translationRepository.get(arenaData.arenaTableName, arenaData.arenaId)
		translationData!!.arenaId shouldBe arenaId
		translationData.ignored shouldBe true

		repository.get(arenaData.arenaTableName, AmtOperation.CREATED, opPos).ingestStatus shouldBe IngestStatus.IGNORED

	}

	@Test
	fun `handleEntry() - operation type delete - legges til i failed liste`() {
		val arenaId = "1234567"
		val opPos = "11223344"

		val arenaData = ArenaData(
			arenaTableName =  TILTAKGJENNOMFORING_TABLE_NAME,
			arenaId =  arenaId,
			operation =  AmtOperation.DELETED,
			operationPosition =  opPos,
			operationTimestamp =  LocalDateTime.now(),
			before = arenaGjennomforingJson,
		)

		gjennomforingProcessor.handle(arenaData)

		val translationData = translationRepository.get(arenaData.arenaTableName, arenaData.arenaId)
		translationData shouldBe null

		repository.get(arenaData.arenaTableName, AmtOperation.DELETED, opPos).ingestStatus shouldBe IngestStatus.FAILED
	}

	val arenaGjennomforingUgyldigJson = mapper.readTree("""
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
	""".trimIndent())

	val arenaGjennomforingUkjentTypeJson = mapper.readTree("""
		{
		  "TILTAKGJENNOMFORING_ID": 3728063,
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
	""".trimIndent())

	val arenaGjennomforingJson = mapper.readTree("""
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
	""".trimIndent())
}

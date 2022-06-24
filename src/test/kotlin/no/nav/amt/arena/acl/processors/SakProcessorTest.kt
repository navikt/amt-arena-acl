package no.nav.amt.arena.acl.processors

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaSak
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaSakKafkaMessage
import no.nav.amt.arena.acl.exceptions.IgnoredException
import no.nav.amt.arena.acl.integration.commands.Command.Companion.objectMapper
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.ArenaGjennomforingRepository
import no.nav.amt.arena.acl.repositories.ArenaSakRepository
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.utils.ARENA_SAK_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime

class SakProcessorTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var sakProcessor: SakProcessor
	lateinit var arenaDataRepository: ArenaDataRepository
	lateinit var arenaSakRepository: ArenaSakRepository
	lateinit var arenaGjennomforingRepository: ArenaGjennomforingRepository
	lateinit var kafkaProducerService: KafkaProducerService

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		val template = NamedParameterJdbcTemplate(dataSource)
		arenaDataRepository = ArenaDataRepository(template)
		arenaSakRepository = ArenaSakRepository(template)
		arenaGjennomforingRepository = ArenaGjennomforingRepository(template)
		kafkaProducerService = mockk()
		sakProcessor = SakProcessor(arenaDataRepository, arenaSakRepository, arenaGjennomforingRepository, kafkaProducerService, mockk())

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("Skal inserte sak hvis sak ikke allerede finnes") {
		val sakId = 123L

		val data = createArenaSakKafkaMessage(
			operationPosition = "12345",
			arenaSak = createArenaSak(sakId, 2020, 32, "1234")
		)

		arenaSakRepository.hentSakMedArenaId(sakId) shouldBe null

		sakProcessor.handleArenaMessage(data)

		val sak = arenaSakRepository.hentSakMedArenaId(sakId)

		sak shouldNotBe null
		sak?.aar shouldBe 2020
		sak?.lopenr shouldBe 32
	}

	test("Skal upserte sak hvis sak allerede finnes") {
		val sakId = 123L

		val data = createArenaSakKafkaMessage(
			operationPosition = "12345",
			arenaSak = createArenaSak(sakId, 2020, 32, "5432")
		)

		arenaSakRepository.upsertSak(sakId, 2020, 32, "1234")

		sakProcessor.handleArenaMessage(data)

		val sak = arenaSakRepository.hentSakMedArenaId(sakId)

		sak?.ansvarligEnhetId shouldBe "5432"
	}

	test("Skal kaste IgnoredException for sakskode som ikke er relevant") {
		val sakId = 123L

		val data = createArenaSakKafkaMessage(
			operationPosition = "12345",
			arenaSak = createArenaSak(sakId, 2020, 32, "1234", "ARBEID")
		)

		println(objectMapper.writeValueAsString(data))

		shouldThrowExactly<IgnoredException> {
			sakProcessor.handleArenaMessage(data)
		}
	}

})

private fun createArenaSak(
	sakId: Long,
	aar: Int,
	lopenr: Int,
	ansvarligEnhetId: String,
	sakskode: String = "TILT",
): ArenaSak {
	return ArenaSak(
		SAK_ID = sakId,
		SAKSKODE = sakskode,
		TABELLNAVNALIAS = "",
		OBJEKT_ID = null,
		AAR = aar,
		LOPENRSAK = lopenr,
		DATO_AVSLUTTET = "",
		SAKSTATUSKODE = "",
		AETATENHET_ARKIV = "",
		ARKIVHENVISNING = "",
		BRUKERID_ANSVARLIG = "",
		AETATENHET_ANSVARLIG = ansvarligEnhetId,
		OBJEKT_KODE = null,
		STATUS_ENDRET = null,
		PARTISJON = null,
		ER_UTLAND = "",
		REG_DATO = "",
		REG_USER = "",
		MOD_DATO = "",
		MOD_USER = "",
		ARKIVNOKKEL = 123,
	)
}

private fun createArenaSakKafkaMessage(
	operationPosition: String = "1",
	operationType: AmtOperation = AmtOperation.CREATED,
	operationTimestamp: LocalDateTime = LocalDateTime.now(),
	arenaSak: ArenaSak,
): ArenaSakKafkaMessage {
	return ArenaSakKafkaMessage(
		arenaTableName =  ARENA_SAK_TABLE_NAME,
		operationType = operationType,
		operationTimestamp = operationTimestamp,
		operationPosition =  operationPosition,
		before = if (listOf(AmtOperation.MODIFIED, AmtOperation.DELETED).contains(operationType)) arenaSak else null,
		after =  if (listOf(AmtOperation.CREATED, AmtOperation.MODIFIED).contains(operationType)) arenaSak else null,
	)
}

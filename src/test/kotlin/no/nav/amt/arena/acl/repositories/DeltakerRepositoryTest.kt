package no.nav.amt.arena.acl.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.asLocalDate
import no.nav.amt.arena.acl.utils.asLocalDateTime
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class DeltakerRepositoryTest : FunSpec({
	val dataSource = SingletonPostgresContainer.getDataSource()
	lateinit var repository: DeltakerRepository

	beforeEach {
		repository = DeltakerRepository(NamedParameterJdbcTemplate(dataSource))
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("upsert - skal upserte ny deltaker") {
		val arenaId = 34234L
		val dbo = DeltakerInsertDbo(
			arenaId = arenaId,
			personId = 5543534,
			gjennomforingId = 32354,
			datoFra = "2016-03-01 00:00:00".asLocalDate(),
			datoTil = "2016-03-02 00:00:00".asLocalDate(),
			regDato = "2016-03-17 14:21:17".asLocalDateTime(),
			modDato = "2016-03-18 14:21:17".asLocalDateTime(),
			status = "status",
			datoStatusEndring = "2018-03-18 14:21:17".asLocalDateTime(),
			eksternId = UUID.randomUUID(),
			arenaSourceTable = ARENA_DELTAKER_TABLE_NAME,
		)
		repository.upsert(dbo)

		val deltaker = repository.get(arenaId, ARENA_DELTAKER_TABLE_NAME)
		deltaker!!.toInsertDbo() shouldBe dbo
	}

	test("upsert - skal oppdatere") {
		val arenaId = 34234L
		val dbo = DeltakerInsertDbo(
			arenaId = arenaId,
			personId = 5543534,
			gjennomforingId = 32354,
			datoFra = "2016-03-01 00:00:00".asLocalDate(),
			datoTil = "2016-03-02 00:00:00".asLocalDate(),
			regDato = "2016-03-17 14:21:17".asLocalDateTime(),
			modDato = "2016-03-18 14:21:17".asLocalDateTime(),
			status = "status",
			datoStatusEndring = "2018-03-18 14:21:17".asLocalDateTime(),
			eksternId = UUID.randomUUID(),
			arenaSourceTable = ARENA_DELTAKER_TABLE_NAME,
		)
		val dbo2 = dbo.copy(datoFra = "2016-02-01 00:00:00".asLocalDate())
		repository.upsert(dbo)
		repository.upsert(dbo2)


		val deltaker = repository.get(arenaId, ARENA_DELTAKER_TABLE_NAME)
		deltaker!!.toInsertDbo() shouldBe dbo2
	}

})

fun DeltakerDbo.toInsertDbo(): DeltakerInsertDbo = DeltakerInsertDbo(
	arenaId = arenaId,
	personId = personId,
	gjennomforingId = gjennomforingId,
	datoFra = datoFra,
	datoTil = datoTil,
	regDato = regDato,
	modDato = modDato,
	status = status,
	datoStatusEndring = datoStatusEndring,
	arenaSourceTable = arenaSourceTable,
	eksternId = eksternId,
)

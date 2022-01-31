package no.nav.amt.arena.acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.ArenaData
import no.nav.amt.arena.acl.domain.IngestStatus
import no.nav.amt.arena.acl.domain.amt.AmtOperation
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime

class ArenaDataRepositoryTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()
	val mapper = jacksonObjectMapper()


	lateinit var repository: ArenaDataRepository

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		repository = ArenaDataRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("Insert and get should return inserted object") {
		val afterData = mapper.readTree("{\"test\": \"test\"}".toByteArray())

		val data = ArenaData(
			arenaTableName = "Table",
			arenaId = "ARENA_ID",
			operation = AmtOperation.CREATED,
			operationPosition = "1",
			operationTimestamp = LocalDateTime.now(),
			after = afterData
		)

		repository.upsert(data)

		val stored = repository.get(data.arenaTableName, data.operation, data.operationPosition)

		stored shouldNotBe null
		stored.id shouldNotBe -1
		stored.arenaId shouldBe data.arenaId
		stored.before shouldBe null
		stored.after shouldBe afterData
	}

	test("Upserting a Inserted object should modify it") {
		val data = ArenaData(
			arenaTableName = "Table",
			arenaId = "ARENA_ID",
			operation = AmtOperation.CREATED,
			operationPosition = "1",
			operationTimestamp = LocalDateTime.now(),
			after = mapper.readTree("{\"test\": \"test\"}".toByteArray())
		)

		repository.upsert(data)

		val stored = repository.get(data.arenaTableName, data.operation, data.operationPosition)

		repository.upsert(stored.retry())

		val updated = repository.get(data.arenaTableName, data.operation, data.operationPosition)

		stored.id shouldBe updated.id
		updated.ingestStatus shouldBe IngestStatus.RETRY
		updated.ingestAttempts shouldBe 1
		updated.lastAttempted shouldNotBe null
	}

	test("Should delete all ignored arena data") {
		val afterData = mapper.readTree("{\"test\": \"test\"}".toByteArray())

		val data1 = ArenaData(
			arenaTableName = "Table",
			arenaId = "ARENA_ID",
			operation = AmtOperation.CREATED,
			operationPosition = "1",
			operationTimestamp = LocalDateTime.now(),
			after = afterData
		)

		val data2 = ArenaData(
			arenaTableName = "Table",
			arenaId = "ARENA_ID",
			operation = AmtOperation.CREATED,
			operationPosition = "2",
			operationTimestamp = LocalDateTime.now(),
			ingestStatus = IngestStatus.IGNORED,
			after = afterData
		)

		val data3 = ArenaData(
			arenaTableName = "Table",
			arenaId = "ARENA_ID",
			operation = AmtOperation.CREATED,
			operationPosition = "3",
			operationTimestamp = LocalDateTime.now(),
			ingestStatus = IngestStatus.IGNORED,
			after = afterData
		)

		repository.upsert(data1)
		repository.upsert(data2)
		repository.upsert(data3)

		val rowsDeleted = repository.deleteAllIgnoredData()

		val allData = repository.getAll()

		rowsDeleted shouldBe 2
		allData.size shouldBe 1
	}

})

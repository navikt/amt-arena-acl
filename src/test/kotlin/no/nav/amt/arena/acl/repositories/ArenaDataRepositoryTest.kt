package no.nav.amt.arena.acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.db.ArenaDataUpsertInput
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.utils.DbUtils.isEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime


class ArenaDataRepositoryTest {

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var repository: ArenaDataRepository

	@BeforeEach
	internal fun setUp() {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		repository = ArenaDataRepository(NamedParameterJdbcTemplate(dataSource))

		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@Test
	internal fun `Can insert all InsertStatuses`() {
		var operationPosition = 0

		IngestStatus.values().forEach { ingestStatus ->
			val data = arenaDataUpsertInput(
				ingestStatus = ingestStatus,
				operationPosition = "${operationPosition++}"
			)
			repository.upsert(data)

			val stored = repository.get(data.arenaTableName, data.operation, data.operationPosition)
			stored.ingestStatus shouldBe ingestStatus
		}
	}

	@Test
	internal fun `Insert and get should return inserted object`() {
		val data = arenaDataUpsertInput()
		repository.upsert(arenaDataUpsertInput())

		val stored = repository.get(data.arenaTableName, data.operation, data.operationPosition)

		stored shouldNotBe null
		stored.id shouldNotBe -1
		stored.arenaId shouldBe data.arenaId
		stored.before shouldBe null
		stored.after shouldBe data.after
	}

	@Test
	internal fun `Upserting a Inserted object should modify it`() {
		val data = arenaDataUpsertInput()
		repository.upsert(data)

		val stored = repository.get(data.arenaTableName, data.operation, data.operationPosition)

		val newIngestedTimestamp = LocalDateTime.now()

		val data2 = arenaDataUpsertInput(
			ingestStatus = IngestStatus.RETRY,
			ingestedTimestamp = newIngestedTimestamp,
			note = "some note"
		)

		repository.upsert(data2)
		val updated = repository.get(data.arenaTableName, data.operation, data.operationPosition)

		stored.id shouldBe updated.id
		updated.ingestStatus shouldBe IngestStatus.RETRY
		updated.ingestedTimestamp!!.isEqualTo(newIngestedTimestamp) shouldBe true
		updated.note shouldBe "some note"
	}

	@Test
	internal fun `getByArenaId returns all messages with arenaId`() {
		repository.upsert(arenaDataUpsertInput(operationPosition = "1"))
		repository.upsert(arenaDataUpsertInput(operationPosition = "2"))
		repository.upsert(arenaDataUpsertInput(operationPosition = "3", arenaId = "SHOULD NOT BE THERE"))
		repository.upsert(arenaDataUpsertInput(operationPosition = "4", arenaTableName = "SHOULD NOT BE THERE"))

		val dataWithId = repository.getByArenaId(
			tableName = "Table",
			arenaId = "ARENA_ID"
		)

		dataWithId.size shouldBe 2
		dataWithId.map { it.arenaId } shouldNotContain "SHOULD NOT BE THERE"
		dataWithId.map { it.arenaTableName } shouldNotContain "SHOULD NOT BE THERE"
	}

	@Test
	internal fun `Should delete all ignored arena data`() {
		val data1 = arenaDataUpsertInput()

		val data2 = arenaDataUpsertInput(
			ingestStatus = IngestStatus.IGNORED,
			operationPosition = "2"
		)

		val data3 = arenaDataUpsertInput(
			ingestStatus = IngestStatus.IGNORED,
			operationPosition = "3"
		)

		repository.upsert(data1)
		repository.upsert(data2)
		repository.upsert(data3)

		val rowsDeleted = repository.deleteAllIgnoredData()

		val allData = repository.getAll()

		rowsDeleted shouldBe 2
		allData.size shouldBe 1
	}

	fun arenaDataUpsertInput(
		ingestStatus: IngestStatus = IngestStatus.NEW,
		ingestedTimestamp: LocalDateTime? = null,
		operationPosition: String = "1",
		arenaTableName: String = "Table",
		arenaId: String = "ARENA_ID",
		note: String? = null
	): ArenaDataUpsertInput = ArenaDataUpsertInput(
		arenaTableName = arenaTableName,
		arenaId = arenaId,
		operation = AmtOperation.CREATED,
		ingestedTimestamp = ingestedTimestamp,
		ingestStatus = ingestStatus,
		note = note,
		operationPosition = operationPosition,
		operationTimestamp = LocalDateTime.now(),
		after = "{\"test\": \"test\"}"
	)

}

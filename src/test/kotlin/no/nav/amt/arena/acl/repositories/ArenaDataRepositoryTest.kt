package no.nav.amt.arena.acl.repositories

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.domain.db.ArenaDataUpsertInput
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.utils.DbUtils.isEqualTo
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest(classes = [ArenaDataRepository::class])
class ArenaDataRepositoryTest(
	dataRepository: ArenaDataRepository,
) : KotestRepositoryTestBase({
	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN
	}

	test("getFailedIngestStatusCount skal returnere 0 når det ikke er feil") {
		dataRepository.upsert(arenaDataUpsertInputInTest)
		val failedCount = dataRepository.getFailedIngestStatusCount()
		failedCount shouldBe 0
	}

	test("getFailedIngestStatusCount skal returnere antall feil når det finnes feil") {
		dataRepository.upsert(arenaDataUpsertInputInTest.copy(ingestStatus = IngestStatus.FAILED))
		val failedCount = dataRepository.getFailedIngestStatusCount()
		failedCount shouldBe 1
	}

	test("Insert and get should return inserted object") {
		dataRepository.upsert(arenaDataUpsertInputInTest)

		val stored = dataRepository.get(
			tableName = arenaDataUpsertInputInTest.arenaTableName,
			operation = arenaDataUpsertInputInTest.operation,
			position = arenaDataUpsertInputInTest.operationPosition
		)

		assertSoftly(stored.shouldNotBeNull()) {
			id shouldNotBe -1
			arenaId shouldBe arenaDataUpsertInputInTest.arenaId
			before shouldBe null
			after shouldBe arenaDataUpsertInputInTest.after
		}
	}

	test("retryDeltakereMedGjennomforingIdOgStatus - skal ikke feile") {
		val gjennomforingId = 123

		val data = arenaDataUpsertInputInTest.copy(
			ingestStatus = IngestStatus.WAITING,
			after = "{\"TILTAKGJENNOMFORING_ID\": \"$gjennomforingId\"}"
		)

		dataRepository.upsert(data)

		dataRepository.retryDeltakereMedGjennomforingIdOgStatus(
			arenaGjennomforingId = gjennomforingId.toString(),
			statuses = listOf(IngestStatus.WAITING)
		)
	}

	test("Upserting an existing object should modify it") {
		val data = arenaDataUpsertInputInTest.copy()

		dataRepository.upsert(data)

		val stored = dataRepository.get(
			tableName = data.arenaTableName,
			operation = data.operation,
			position = data.operationPosition
		)

		stored.shouldNotBeNull()

		val newIngestedTimestamp = LocalDateTime.now()

		val data2 =
			data.copy(
				ingestStatus = IngestStatus.RETRY,
				ingestedTimestamp = newIngestedTimestamp,
				note = "some note",
			)

		dataRepository.upsert(data2)

		val updated = dataRepository.get(
			tableName = data.arenaTableName,
			operation = data.operation,
			position = data.operationPosition
		)

		assertSoftly(updated.shouldNotBeNull()) {
			ingestStatus shouldBe IngestStatus.RETRY
			note shouldBe "some note"

			ingestedTimestamp.shouldNotBeNull()
			ingestedTimestamp.isEqualTo(newIngestedTimestamp) shouldBe true
		}

		stored.id shouldBe updated.id
	}

	test("Should delete all ignored arena data") {
		setOf(
			arenaDataUpsertInputInTest.copy(),
			arenaDataUpsertInputInTest.copy(
				operationPosition = "2",
				ingestStatus = IngestStatus.IGNORED,
			),
			arenaDataUpsertInputInTest.copy(
				operationPosition = "3",
				ingestStatus = IngestStatus.IGNORED,
			)
		).forEach { dataRepository.upsert(it) }

		val rowsDeleted = dataRepository.deleteAllIgnoredData()
		rowsDeleted shouldBe 2

		val allData = dataRepository.getAll()
		allData.size shouldBe 1
	}
}) {
	companion object {
		private val arenaDataUpsertInputInTest =
			ArenaDataUpsertInput(
				arenaTableName = "Table",
				arenaId = "ARENA_ID",
				operation = AmtOperation.CREATED,
				operationPosition = "1",
				operationTimestamp = LocalDateTime.now(),
				after = "{\"test\": \"test\"}",
			)
	}
}

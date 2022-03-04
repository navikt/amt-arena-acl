package no.nav.amt.arena.acl.processors

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.TiltakRepository
import no.nav.amt.arena.acl.services.TiltakService
import no.nav.amt.arena.acl.utils.ARENA_TILTAK_TABLE_NAME
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

class TiltakProcessorTest : FunSpec({

	val dataSource = SingletonPostgresContainer.getDataSource()

	lateinit var arenaDataRepository: ArenaDataRepository
	lateinit var tiltakRepository: TiltakRepository

	lateinit var tiltakProcessor: TiltakProcessor

	beforeEach {
		val rootLogger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN

		val template = NamedParameterJdbcTemplate(dataSource)

		arenaDataRepository = ArenaDataRepository(template)
		tiltakRepository = TiltakRepository(template)
		DatabaseTestUtils.cleanDatabase(dataSource)

		tiltakProcessor = TiltakProcessor(arenaDataRepository, TiltakService(tiltakRepository))
	}

	test("Add Tiltak") {
		val position = UUID.randomUUID().toString()
		val tiltakKode = "Tiltak1_KODE"
		val tiltakNavn = "Tiltak1_NAVN"

		val data = createNewTiltakArenaData(position, tiltakKode, tiltakNavn)

		tiltakProcessor.handleArenaMessage(data)

		val arenaDataRepositoryEntry = shouldNotThrowAny {
			arenaDataRepository.get(ARENA_TILTAK_TABLE_NAME, AmtOperation.CREATED, position)
		}

		arenaDataRepositoryEntry.before shouldBe null
		arenaDataRepositoryEntry.after shouldBe data.after
		arenaDataRepositoryEntry.operation shouldBe AmtOperation.CREATED
		arenaDataRepositoryEntry.id shouldNotBe -1

		arenaDataRepositoryEntry.ingestStatus shouldBe IngestStatus.HANDLED
		arenaDataRepositoryEntry.ingestedTimestamp shouldNotBe null
		arenaDataRepositoryEntry.ingestAttempts shouldBe 0


		val tiltakRepositoryEntry = tiltakRepository.getByKode(tiltakKode)

		tiltakRepositoryEntry shouldNotBe null
		tiltakRepositoryEntry!!.id shouldNotBe null
		tiltakRepositoryEntry.kode shouldBe tiltakKode
		tiltakRepositoryEntry.navn shouldBe tiltakNavn
	}

	test("Update Tiltak") {
		val newPosition = UUID.randomUUID().toString()
		val tiltakKode = "Tiltak1_KODE"
		val tiltakNavn = "Tiltak1_NAVN"

		val insert = createNewTiltakArenaData(newPosition, tiltakKode, tiltakNavn)
		tiltakProcessor.handle(insert)

		val updatedPosition = UUID.randomUUID().toString()
		val updatedNavn = "TILTAK1_UPDATED_NAVN"

		val update = createUpdateTiltakArenaData(updatedPosition, insert.after!!, tiltakKode, updatedNavn)
		tiltakProcessor.handle(update)

		val arenaDataRepositoryEntry = shouldNotThrowAny {
			arenaDataRepository.get(ARENA_TILTAK_TABLE_NAME, AmtOperation.MODIFIED, updatedPosition)
		}

		arenaDataRepositoryEntry.before shouldBe update.before
		arenaDataRepositoryEntry.after shouldBe update.after
		arenaDataRepositoryEntry.operation shouldBe AmtOperation.MODIFIED
		arenaDataRepositoryEntry.id shouldNotBe -1

		arenaDataRepositoryEntry.ingestStatus shouldBe IngestStatus.HANDLED
		arenaDataRepositoryEntry.ingestedTimestamp shouldNotBe null
		arenaDataRepositoryEntry.ingestAttempts shouldBe 0


		val tiltakRepositoryEntry = tiltakRepository.getByKode(tiltakKode)

		tiltakRepositoryEntry shouldNotBe null
		tiltakRepositoryEntry!!.id shouldNotBe null
		tiltakRepositoryEntry.kode shouldBe tiltakKode
		tiltakRepositoryEntry.navn shouldBe updatedNavn
	}

	test("Delete Tiltak") {
		val newPosition = UUID.randomUUID().toString()
		val tiltakKode = "Tiltak1_KODE"
		val tiltakNavn = "Tiltak1_NAVN"

		val insert = createNewTiltakArenaData(newPosition, tiltakKode, tiltakNavn)
		tiltakProcessor.handle(insert)

		val deletePosition = UUID.randomUUID().toString()
		val delete = createDeleteTiltakArenaData(deletePosition, insert.after!!)
		tiltakProcessor.handle(delete)

		val arenaDataRepositoryEntry = shouldNotThrowAny {
			arenaDataRepository.get(ARENA_TILTAK_TABLE_NAME, AmtOperation.DELETED, deletePosition)
		}

		arenaDataRepositoryEntry.ingestStatus shouldBe IngestStatus.FAILED
	}


})

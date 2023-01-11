package no.nav.amt.arena.acl.repositories

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class GjennomforingRepositoryTest {
	val dataSource = SingletonPostgresContainer.getDataSource()
	val repository: GjennomforingRepository = GjennomforingRepository(NamedParameterJdbcTemplate(dataSource))

	@BeforeEach
	fun before() {
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	@Test
	fun `upsert - skal upserte gjennomforing`() {
		val arenaId = "ARENA_ID"
		repository.upsert(arenaId,"INDOPPFAG", true)

		repository.isValid(arenaId) shouldBe true
	}

	@Test
	fun `upsert - skal oppdatere eksisterende`() {
		val arenaId = "ARENA_ID"
		repository.upsert(arenaId,"INDOPPFAG", false)
		repository.upsert(arenaId,"INDOPPFAG", true)


		repository.isValid(arenaId) shouldBe true
	}

	@Test
	fun `isValid - skal hente riktig record - flere er tilgjengelig`() {
		val arenaId1 = "ARENA_ID1"
		val arenaId2 = "ARENA_ID2"
		repository.upsert(arenaId1,"INDOPPFAG", true)
		repository.upsert(arenaId2,"INDOPPFAG", false)

		repository.isValid(arenaId1) shouldBe true
		repository.isValid(arenaId2) shouldBe false

	}
}

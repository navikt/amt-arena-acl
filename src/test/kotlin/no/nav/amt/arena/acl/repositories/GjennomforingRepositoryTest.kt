package no.nav.amt.arena.acl.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.database.DatabaseTestUtils
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class GjennomforingRepositoryTest : FunSpec({
	val dataSource = SingletonPostgresContainer.getDataSource()
	lateinit var repository: GjennomforingRepository

	beforeEach {
		repository = GjennomforingRepository(NamedParameterJdbcTemplate(dataSource))
		DatabaseTestUtils.cleanDatabase(dataSource)
	}

	test("upsert - skal upserte gjennomforing"){
		val arenaId = "ARENA_ID"
		repository.upsert(arenaId,"INDOPPFAG", true)

		repository.isValid(arenaId) shouldBe true
	}

	test("upsert - skal oppdatere eksisterende") {
		val arenaId = "ARENA_ID2"
		repository.upsert(arenaId,"INDOPPFAG", false)
		repository.upsert(arenaId,"INDOPPFAG", true)

		repository.isValid(arenaId) shouldBe true
	}

	test("isValid - skal hente riktig record - flere er tilgjengelig") {
		val arenaId1 = "ARENA_ID3"
		val arenaId2 = "ARENA_ID4"
		repository.upsert(arenaId1,"INDOPPFAG", true)
		repository.upsert(arenaId2,"INDOPPFAG", false)

		repository.isValid(arenaId1) shouldBe true
		repository.isValid(arenaId2) shouldBe false

	}
})

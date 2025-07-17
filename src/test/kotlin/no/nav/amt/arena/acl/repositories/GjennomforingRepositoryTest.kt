package no.nav.amt.arena.acl.repositories

import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@SpringBootTest(classes = [GjennomforingRepository::class])
class GjennomforingRepositoryTest(
	gjennomforingRepository: GjennomforingRepository,
) : KotestRepositoryTestBase({
		test("upsert - skal upserte gjennomforing") {
			val arenaId = "ARENA_ID"
			gjennomforingRepository.upsert(arenaId, "INDOPPFAG", true)

			gjennomforingRepository.get(arenaId)!!.isValid shouldBe true
		}

		test("get - skal oppdatere eksisterende") {
			val arenaId = "ARENA_ID2"
			gjennomforingRepository.upsert(arenaId, "INDOPPFAG", false)
			gjennomforingRepository.upsert(arenaId, "INDOPPFAG", true)

			gjennomforingRepository.get(arenaId)!!.isValid shouldBe true
		}

		test("get - skal hente riktig record - flere er tilgjengelig") {
			val arenaId1 = "ARENA_ID3"
			val arenaId2 = "ARENA_ID4"
			gjennomforingRepository.upsert(arenaId1, "INDOPPFAG", true)
			gjennomforingRepository.upsert(arenaId2, "INDOPPFAG", false)

			gjennomforingRepository.get(arenaId1)!!.isValid shouldBe true
			gjennomforingRepository.get(arenaId2)!!.isValid shouldBe false
		}

		test("updateGjennomforingId - record finnes - oppdaterer id") {
			val arenaId1 = "ARENA_ID3"
			val mrId = UUID.randomUUID()
			gjennomforingRepository.upsert(arenaId1, "INDOPPFAG", true)
			gjennomforingRepository.updateGjennomforingId(arenaId1, mrId)

			gjennomforingRepository.get(arenaId1)!!.id shouldBe mrId
		}
	})

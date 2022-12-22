package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.services.ToggleService
import no.nav.amt.arena.acl.utils.JsonUtils.toJsonString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class DeltakerIntegrationTest : IntegrationTestBase() {

	@Autowired
	lateinit var toggleService: ToggleService

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender


	companion object {
		@JvmStatic
		@DynamicPropertySource
		fun startEnvironment(registry: DynamicPropertyRegistry) {
			setupEnvironment(registry)
			registry.add("toggle.hent_gjennomforing_fra_mulighetsrommet") { "true" }
		}
	}

	@Test
	fun `ingest deltaker`() {
		val gjennomforingMessage = KafkaMessageCreator.opprettArenaGjennomforing(
			KafkaMessageCreator.baseGjennomforing(
				arenaGjennomforingId = 123,
				tiltakskode = "INDOPPFAG",
				navn = "test",
				arbgivIdArrangor = 65464,
				tiltakstatuskode = "GJENNOMFOR"
			)
		)

		kafkaMessageSender.publiserArenaGjennomforing("123", toJsonString(gjennomforingMessage))



		toggleService.hentGjennomforingFraMulighetsrommetEnabled() shouldBe true
	}


}

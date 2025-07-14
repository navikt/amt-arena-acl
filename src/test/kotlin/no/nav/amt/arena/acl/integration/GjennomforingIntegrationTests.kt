package no.nav.amt.arena.acl.integration

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class GjennomforingIntegrationTests(
	private val kafkaMessageSender: KafkaMessageSender,
	private val gjennomforingService: GjennomforingService,
	private val arenaDataRepository: ArenaDataRepository,
) : IntegrationTestBase() {
	@BeforeEach
	fun setup() = mockArenaOrdsProxyHttpServer.mockHentVirksomhetsnummer(
		arenaArbeidsgiverId = "0",
		virksomhetsnummer = "12345"
	)

	@Test
	fun `Konsumer gjennomføring - gyldig gjennomføring - ingestes uten feil`() {
		val gjennomforing = createGjennomforing()
		val gjennomforingId = UUID.randomUUID()
		mockMulighetsrommetApiServer.mockHentGjennomforingId(gjennomforing.TILTAKGJENNOMFORING_ID, gjennomforingId)
		val pos = (1..Long.MAX_VALUE).random().toString()

		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforing.TILTAKGJENNOMFORING_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing, opPos = pos)),
		)

		await().untilAsserted {
			val gjennomforingResult = gjennomforingService.get(gjennomforing.TILTAKGJENNOMFORING_ID.toString())
			assertSoftly(gjennomforingResult.shouldNotBeNull()) {
				isValid shouldBe true
				isSupported shouldBe true
				id shouldBe gjennomforingId
			}

			val arenaDataDbo =
				arenaDataRepository
					.get(
						ARENA_GJENNOMFORING_TABLE_NAME,
						AmtOperation.CREATED,
						pos,
					)
			arenaDataDbo.shouldNotBeNull()
			arenaDataDbo.ingestStatus shouldBe IngestStatus.HANDLED
		}
	}

	@Test
	fun `Konsumer gjennomføring - ugyldig gjennomføring - får status invalid`() {
		val ugyldigGjennomforing = createGjennomforing().copy(ARBGIV_ID_ARRANGOR = null)
		val gjennomforingId = UUID.randomUUID()

		mockMulighetsrommetApiServer.mockHentGjennomforingId(
			ugyldigGjennomforing.TILTAKGJENNOMFORING_ID,
			gjennomforingId,
		)
		val pos = (1..Long.MAX_VALUE).random().toString()

		kafkaMessageSender.publiserArenaGjennomforing(
			ugyldigGjennomforing.TILTAKGJENNOMFORING_ID,
			JsonUtils.toJsonString(
				KafkaMessageCreator.opprettArenaGjennomforingMessage(
					ugyldigGjennomforing,
					opPos = pos,
				),
			),
		)

		await().untilAsserted {
			val gjennomforingResult =
				gjennomforingService.get(ugyldigGjennomforing.TILTAKGJENNOMFORING_ID.toString())

			assertSoftly(gjennomforingResult.shouldNotBeNull()) {
				isValid shouldBe false
				isSupported shouldBe true
				id shouldBe gjennomforingId
			}

			val arenaDataDbo =
				arenaDataRepository
					.get(
						ARENA_GJENNOMFORING_TABLE_NAME,
						AmtOperation.CREATED,
						pos,
					)
			arenaDataDbo.shouldNotBeNull()
			arenaDataDbo.ingestStatus shouldBe IngestStatus.HANDLED
		}
	}

	@Test
	fun `Konsumer gjennomføring - tiltakstype er ikke støttet - lagrer med korrekte verdier`() {
		val gjennomforing = createGjennomforing().copy(TILTAKSKODE = "IKKE STØTTET")
		val pos = (1..Long.MAX_VALUE).random().toString()

		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforing.TILTAKGJENNOMFORING_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing, opPos = pos)),
		)

		await().untilAsserted {
			val gjennomforingResult = gjennomforingService.get(gjennomforing.TILTAKGJENNOMFORING_ID.toString())
			assertSoftly(gjennomforingResult.shouldNotBeNull()) {
				isValid shouldBe true
				isSupported shouldBe false
			}

			arenaDataRepository
				.get(
					ARENA_GJENNOMFORING_TABLE_NAME,
					AmtOperation.CREATED,
					pos,
				).shouldBeNull()
		}
	}

	private fun createGjennomforing(arenaId: Long = 34524534543) =
		KafkaMessageCreator.baseGjennomforing(
			arenaGjennomforingId = arenaId,
			arbgivIdArrangor = 68968L,
			datoFra = LocalDateTime.now().minusDays(3),
			datoTil = LocalDateTime.now().plusDays(3),
		)
}

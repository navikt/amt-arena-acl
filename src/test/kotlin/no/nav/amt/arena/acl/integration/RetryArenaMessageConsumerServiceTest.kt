package no.nav.amt.arena.acl.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.clients.mulighetsrommetapi.Gjennomforing
import no.nav.amt.arena.acl.domain.db.IngestStatus
import no.nav.amt.arena.acl.domain.kafka.amt.AmtOperation
import no.nav.amt.arena.acl.domain.kafka.arena.ArenaDeltaker
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageConsumer
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageCreator
import no.nav.amt.arena.acl.integration.kafka.KafkaMessageSender
import no.nav.amt.arena.acl.integration.utils.AsyncUtils
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.services.RetryArenaMessageProcessorService
import no.nav.amt.arena.acl.utils.ARENA_DELTAKER_TABLE_NAME
import no.nav.amt.arena.acl.utils.ARENA_GJENNOMFORING_TABLE_NAME
import no.nav.amt.arena.acl.utils.JsonUtils
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class RetryArenaMessageConsumerServiceTest(
	private val kafkaMessageSender: KafkaMessageSender,
	private val arenaDataRepository: ArenaDataRepository,
	private val retryArenaMessageProcessorService: RetryArenaMessageProcessorService,
	private val gjennomforingService: GjennomforingService,
) : IntegrationTestBase() {
	val gjennomforingArenaId = 5435345L
	val gjennomforingIdMR = UUID.randomUUID()

	@Test
	fun `processMessages - deltaker har status RETRY pga manglende gjennomføring - får status HANDLED når gjennomføring er ingestet`() {
		val deltakere: MutableList<Pair<String, ArenaDeltaker>> = mutableListOf()
		var pos = 1

		repeat(3) {
			val currentPos = pos++.toString()
			val deltaker = publiserDeltaker(currentPos)
			deltakere.add(Pair(currentPos, deltaker))
		}

		publiserGjennomforing(pos++.toString())

		retryArenaMessageProcessorService.processMessages(2)

		deltakere.forEach { deltaker ->
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, deltaker.first)
			println(deltaker)
			arenaData!!.ingestStatus shouldBe IngestStatus.HANDLED
		}

		AsyncUtils.eventually {
			val deltakerRecord = kafkaMessageConsumer.getRecords(KafkaMessageConsumer.Topic.AMT_TILTAK)
			deltakerRecord.size shouldBe 3
		}
	}

	private fun publiserDeltaker(pos: String): ArenaDeltaker {
		val deltaker =
			KafkaMessageCreator.baseDeltaker(
				gjennomforingId = gjennomforingArenaId,
			)
		val gjennomforingData =
			Gjennomforing(
				id = gjennomforingIdMR,
				tiltakstype =
					Gjennomforing.Tiltakstype(
						id = UUID.randomUUID(),
						navn = "Navn på tiltak",
						arenaKode = "INDOPPFAG",
					),
				navn = "navn på gjennomføring",
				startDato = LocalDate.now(),
				sluttDato = LocalDate.now().plusDays(5),
				status = Gjennomforing.Status.GJENNOMFORES,
				virksomhetsnummer = "999888777",
				oppstart = Gjennomforing.Oppstartstype.LOPENDE,
			)
		mockArenaOrdsProxyHttpServer.mockHentFnr(deltaker.PERSON_ID!!, (1..Long.MAX_VALUE).random().toString())
		mockMulighetsrommetApiServer.mockHentGjennomforingData(gjennomforingIdMR, gjennomforingData)

		kafkaMessageSender.publiserArenaDeltaker(
			deltaker.TILTAKDELTAKER_ID,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaDeltaker(arenaDeltaker = deltaker, opPos = pos)),
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_DELTAKER_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.RETRY
		}

		return deltaker
	}

	private fun publiserGjennomforing(pos: String) {
		val gjennomforing =
			KafkaMessageCreator.baseGjennomforing(
				arenaGjennomforingId = gjennomforingArenaId,
				arbgivIdArrangor = 68968L,
				datoFra = LocalDateTime.now().minusDays(3),
				datoTil = LocalDateTime.now().plusDays(3),
			)

		mockMulighetsrommetApiServer.mockHentGjennomforingId(gjennomforingArenaId, gjennomforingIdMR)

		kafkaMessageSender.publiserArenaGjennomforing(
			gjennomforingArenaId,
			JsonUtils.toJsonString(KafkaMessageCreator.opprettArenaGjennomforingMessage(gjennomforing, opPos = pos)),
		)

		AsyncUtils.eventually {
			val arenaData = arenaDataRepository.get(ARENA_GJENNOMFORING_TABLE_NAME, AmtOperation.CREATED, pos)
			arenaData!!.ingestStatus shouldBe IngestStatus.HANDLED
			val gjennomforingResult = gjennomforingService.get(gjennomforing.TILTAKGJENNOMFORING_ID.toString())
			gjennomforingResult shouldNotBe null
		}
	}
}

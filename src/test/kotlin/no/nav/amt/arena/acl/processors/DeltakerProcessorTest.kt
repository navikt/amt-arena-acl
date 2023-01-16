package no.nav.amt.arena.acl.processors

import ArenaOrdsProxyClient
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.amt.arena.acl.clients.mulighetsrommet_api.MulighetsrommetApiClient
import no.nav.amt.arena.acl.domain.db.ArenaDataIdTranslationDbo
import no.nav.amt.arena.acl.domain.kafka.amt.AmtDeltaker
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import no.nav.amt.arena.acl.domain.kafka.amt.AmtKafkaMessageDto
import no.nav.amt.arena.acl.domain.kafka.amt.PayloadType
import no.nav.amt.arena.acl.integration.commands.deltaker.DeltakerInput
import no.nav.amt.arena.acl.metrics.DeltakerMetricHandler
import no.nav.amt.arena.acl.repositories.ArenaDataRepository
import no.nav.amt.arena.acl.repositories.ArenaGjennomforingDbo
import no.nav.amt.arena.acl.services.ArenaDataIdTranslationService
import no.nav.amt.arena.acl.services.GjennomforingService
import no.nav.amt.arena.acl.services.KafkaProducerService
import no.nav.amt.arena.acl.services.ToggleService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeltakerProcessorTest {
	private lateinit var meterRegistry: MeterRegistry
	private lateinit var arenaDataRepository: ArenaDataRepository
	private lateinit var arenaDataIdTranslationService: ArenaDataIdTranslationService
	private lateinit var ordsClient: ArenaOrdsProxyClient
	private lateinit var metrics: DeltakerMetricHandler
	private lateinit var kafkaProducerService: KafkaProducerService
	private lateinit var deltakerProcessor: DeltakerProcessor
	private lateinit var gjennomforingService: GjennomforingService
	private lateinit var mulighetsrommetApiClient: MulighetsrommetApiClient
	private lateinit var toggleService: ToggleService
	val gjennomforingId = UUID.randomUUID()

	@Captor
	private lateinit var kafkaMessageCaptor2: ArgumentCaptor<AmtKafkaMessageDto<*>>

	@Captor
	private lateinit var deltakerIdCaptor: ArgumentCaptor<UUID>

	@BeforeAll
	fun before() {
		meterRegistry = SimpleMeterRegistry()
		arenaDataRepository = mock(ArenaDataRepository::class.java)
		ordsClient = mock(ArenaOrdsProxyClient::class.java)
		arenaDataIdTranslationService = mock(ArenaDataIdTranslationService::class.java)
		metrics = mock(DeltakerMetricHandler::class.java)
		kafkaProducerService = mock(KafkaProducerService::class.java)
		gjennomforingService = mock(GjennomforingService::class.java)
		mulighetsrommetApiClient = mock(MulighetsrommetApiClient::class.java)
		toggleService = mock(ToggleService::class.java)

		deltakerProcessor = DeltakerProcessor(
			meterRegistry, arenaDataRepository, gjennomforingService, arenaDataIdTranslationService, ordsClient, metrics, kafkaProducerService, mulighetsrommetApiClient, toggleService
		)

		kafkaMessageCaptor2 = ArgumentCaptor.forClass(AmtKafkaMessageDto::class.java)
		deltakerIdCaptor = ArgumentCaptor.forClass(UUID::class.java)

		`when`(toggleService.hentGjennomforingFraMulighetsrommetEnabled()).thenReturn(false)
	}


	@Test
	fun `handleArenaMessage() - Skal konvertere deltakerobjekt med korrekte verdier`() {
		val deltakerId = UUID.randomUUID()
		val fnr = "123"
		val gjennomforingArenaId = Random().nextLong()
		val deltakerInput = DeltakerInput(
			tiltakDeltakerId = Random().nextLong(),
			tiltakgjennomforingId = gjennomforingArenaId,
			datoFra = LocalDate.now().minusDays(2),
			datoTil = LocalDate.now().plusDays(1),
			datoStatusEndring = LocalDate.now(),
			deltakerStatusKode = "IKKEM",
			statusAarsak = "SYK"
		)

		`when`(arenaDataIdTranslationService.findGjennomforingIdTranslation(gjennomforingArenaId.toString()))
			.thenReturn(ArenaDataIdTranslationDbo(gjennomforingId, "", ""))

		`when`(gjennomforingService.getGjennomforing(gjennomforingId)).thenReturn(gjennomforing)

		`when`(gjennomforingService.get(deltakerInput.tiltakgjennomforingId.toString())).thenReturn(
			GjennomforingService.Gjennomforing(
				arenaId = deltakerInput.tiltakgjennomforingId.toString(),
				tiltakKode = "INDOPPFAG",
				isValid = true
		))


		`when`(arenaDataIdTranslationService.hentEllerOpprettNyDeltakerId(deltakerInput.tiltakDeltakerId.toString()))
			.thenReturn(deltakerId)

		`when`(ordsClient.hentFnr(deltakerInput.personId.toString())).thenReturn(fnr)

		val arenaKafkaMessage = createArenaDeltakerKafkaMessage(deltakerInput)
		deltakerProcessor.handleArenaMessage(arenaKafkaMessage)

		val amtDeltaker = AmtDeltaker(
			id = deltakerId,
			gjennomforingId = gjennomforingId,
			personIdent = fnr,
			startDato = deltakerInput.datoFra,
			sluttDato = deltakerInput.datoTil,
			status = AmtDeltaker.Status.HAR_SLUTTET,
			statusAarsak = AmtDeltaker.StatusAarsak.SYK,
			dagerPerUke = deltakerInput.antallDagerPerUke,
			prosentDeltid = deltakerInput.prosentDeltid,
			registrertDato = deltakerInput.registrertDato,
			statusEndretDato = deltakerInput.datoStatusEndring.atStartOfDay(),
			innsokBegrunnelse = deltakerInput.innsokBegrunnelse
		)

		verify(kafkaProducerService).sendTilAmtTiltak(capture(deltakerIdCaptor), capture(kafkaMessageCaptor2))

		val capturedDeltaker = kafkaMessageCaptor2.value
		deltakerIdCaptor.value shouldBe deltakerId

		kafkaMessageCaptor2.value shouldNotBe null
		kafkaMessageCaptor2.value.payload shouldBe amtDeltaker
		capturedDeltaker.type shouldBe  PayloadType.DELTAKER

	}

	private fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

	val gjennomforing = ArenaGjennomforingDbo(
		id = gjennomforingId,
		tiltakKode = "INDOPPFAG",
		virksomhetsnummer = "123",
		navn = "Gjennomføringnavn",
		startDato = LocalDate.now(),
		sluttDato = LocalDate.now(),
		registrertDato = LocalDateTime.now(),
		fremmoteDato = LocalDateTime.now(),
		status = AmtGjennomforing.Status.GJENNOMFORES,
		ansvarligNavEnhetId = "1233",
		opprettetAar = 2001,
		lopenr = 902380943,
		arenaSakId = 4892304830924
	)

}

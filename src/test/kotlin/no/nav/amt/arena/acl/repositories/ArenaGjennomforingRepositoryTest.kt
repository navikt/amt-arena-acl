package no.nav.amt.arena.acl.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.date.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.arena.acl.database.SingletonPostgresContainer
import no.nav.amt.arena.acl.domain.kafka.amt.AmtGjennomforing
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.*

class ArenaGjennomforingRepositoryTest : FunSpec({
	val datasource = SingletonPostgresContainer.getDataSource()
	lateinit var repository: ArenaGjennomforingRepository
	val now = LocalDateTime.now()
	beforeEach {
		repository = ArenaGjennomforingRepository(NamedParameterJdbcTemplate( datasource))
	}

	infix fun LocalDateTime.shouldBeCloseTo(other: LocalDateTime) {
		this shouldHaveSameYearAs other
		this shouldHaveSameMonthAs other
		this shouldHaveSameDayAs other
		this shouldHaveHour other.hour
		this shouldHaveMinute other.minute
	}

	infix fun ArenaGjennomforingDbo.shouldCompareTo(other: ArenaGjennomforingDbo) {
		this.registrertDato shouldBeCloseTo other.registrertDato

		if(fremmoteDato == null) other.fremmoteDato shouldBe null
		else this.fremmoteDato!! shouldBeCloseTo other.fremmoteDato!!

	}

	test("upsert - skal inserte ny record") {
		val gjennomforing = ArenaGjennomforingDbo(
			id = UUID.randomUUID(),
			tiltakKode = "INDOPPFAG",
			virksomhetsnummer = "123",
			navn = "Gjennomføringnavn",
			startDato = now.toLocalDate(),
			sluttDato = now.toLocalDate().plusDays(1),
			registrertDato = now,
			fremmoteDato = now,
			status = AmtGjennomforing.Status.GJENNOMFORES,
			ansvarligNavEnhetId = "1233",
			opprettetAar = 2001,
			lopenr = 902380943,
			arenaSakId = 4892304830924
		)
		repository.upsert(gjennomforing)

		val inserted = repository.get(gjennomforing.id)
		inserted shouldNotBe null
		inserted!! shouldCompareTo gjennomforing
	}

	test("upsert - kun obligatoriske felter - skal inserte ny record") {
		val gjennomforing = ArenaGjennomforingDbo(
			id = UUID.randomUUID(),
			arenaSakId = null,
			tiltakKode = "INDOPPFAG",
			virksomhetsnummer = "123",
			navn = "Gjennomføringnavn",
			startDato = null,
			sluttDato = null,
			registrertDato = now,
			fremmoteDato = null,
			status = AmtGjennomforing.Status.GJENNOMFORES,
			ansvarligNavEnhetId = null,
			opprettetAar = null,
			lopenr = null,
		)
		repository.upsert(gjennomforing)

		val inserted = repository.get(gjennomforing.id)

		inserted shouldNotBe null
		inserted!! shouldCompareTo gjennomforing

	}

	test("getBySakId - gjennomføring med sakId eksisterer - skal hente") {
		val gjennomforing = ArenaGjennomforingDbo(
			id = UUID.randomUUID(),
			arenaSakId = 3453453453534,
			tiltakKode = "INDOPPFAG",
			virksomhetsnummer = "123",
			navn = "Gjennomføringnavn",
			startDato = null,
			sluttDato = null,
			registrertDato = now,
			fremmoteDato = null,
			status = AmtGjennomforing.Status.GJENNOMFORES,
			ansvarligNavEnhetId = null,
			opprettetAar = null,
			lopenr = null,
		)
		repository.upsert(gjennomforing)

		val inserted = repository.getBySakId(gjennomforing.arenaSakId!!)

		inserted shouldNotBe null
		inserted!! shouldCompareTo gjennomforing

	}
})

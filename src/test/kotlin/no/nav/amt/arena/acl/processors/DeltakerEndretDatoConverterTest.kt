package no.nav.amt.arena.acl.processors

import io.kotest.matchers.shouldBe
import no.nav.amt.arena.acl.processors.converters.DeltakerEndretDatoConverter
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DeltakerEndretDatoConverterTest {
	private val converter = DeltakerEndretDatoConverter()
	private val now = LocalDateTime.now()

	@Test
	fun `convert() - avsluttende status - endretdato før startdato - returnerer endret dato` () {

		val resultat = converter.convert(
			deltakerStatus = "FULLF",
			datoStatusEndring = now,
			oppstartDato = now.plusDays(1),
			sluttDato = now.plusDays (2)
		)
		resultat shouldBe now
	}

	@Test
	fun `convert() - avsluttende status - sluttdato frem i tid - returnerer endret dato` () {
		val endretDato = now.minusDays(2)
		val resultat = converter.convert(
			deltakerStatus = "FULLF",
			datoStatusEndring = endretDato,
			oppstartDato = now.minusDays(5),
			sluttDato = now.plusDays (2)
		)
		resultat shouldBe endretDato
	}

	@Test
	fun `convert() - avsluttende status - endretdato mellom start og sluttdato - returnerer sluttdato` () {
		val sluttdato = now.minusDays(1)
		val resultat = converter.convert(
			deltakerStatus = "FULLF",
			datoStatusEndring = now.minusDays(2),
			oppstartDato = now.minusDays(3),
			sluttDato = sluttdato
		)
		resultat shouldBe sluttdato
	}

	@Test
	fun `convert() - avsluttende status - endretdato etter start og sluttdato - returnerer sluttdato` () {

		val resultat = converter.convert(
			deltakerStatus = "FULLF",
			datoStatusEndring = now,
			oppstartDato = now.minusDays(2),
			sluttDato = now.minusDays(1)
		)
		resultat shouldBe now.minusDays(1)
	}

	@Test
	fun `convert() - avsluttende status - endretdato etter start og sluttdato mangler - returnerer endretdato` () {

		val resultat = converter.convert(
			deltakerStatus = "FULLF",
			datoStatusEndring = now,
			oppstartDato = now.minusDays(2),
			sluttDato = null
		)
		resultat shouldBe now
	}

	@Test
	fun `convert() - gjennomførende status - startdato har ikke passert - returnerer endretdato` () {

		val resultat = converter.convert(
			deltakerStatus = "GJENN",
			datoStatusEndring = now,
			oppstartDato = now.plusDays(1),
			sluttDato = now.plusDays(2)
		)
		resultat shouldBe now
	}

	@Test
	fun `convert() - gjennomførende status - endrer status mellom start og sluttdato - returnerer startdato` () {
		val oppstartDato = now.minusDays(1)
		val resultat = converter.convert(
			deltakerStatus = "GJENN",
			datoStatusEndring = now,
			oppstartDato = oppstartDato,
			sluttDato = now.plusDays(2)
		)
		resultat shouldBe oppstartDato
	}

	@Test
	fun `convert() - gjennomførende status - startdato og sluttdato har passert - returnerer sluttdato` () {
		val sluttDato = now.minusDays(1)
		val resultat = converter.convert(
			deltakerStatus = "GJENN",
			datoStatusEndring = now,
			oppstartDato = now.minusDays(2),
			sluttDato = sluttDato
		)
		resultat shouldBe sluttDato
	}

	@Test
	fun `convert() - gjennomførende status - startdato passert og sluttdato mangler - returnerer oppstartDato` () {
		val oppstartDato = now.minusDays(2)
		val resultat = converter.convert(
			deltakerStatus = "GJENN",
			datoStatusEndring = now,
			oppstartDato = oppstartDato,
			sluttDato = null
		)
		resultat shouldBe oppstartDato
	}

	@Test
	fun `convert() - ikke aktuell status - mellom start og slutt - returnerer endretdato` () {
		val resultat = converter.convert(
			deltakerStatus = "IKKEAKTUELL",
			datoStatusEndring = now,
			oppstartDato = now.minusDays(1),
			sluttDato = now.plusDays(2)
		)
		resultat shouldBe now
	}

}

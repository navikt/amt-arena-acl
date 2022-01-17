package no.nav.amt.arena.acl.processors

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DeltakerEndretDatoConverterTest {
	private val converter = DeltakerEndretDatoConverter()
	private val endretDato = LocalDateTime.now()

	@Test
	fun `convert() - avsluttende status - endretdato før startdato - returnerer endretdato` () {

		val resultat = converter.convert(
			deltakerStatus = "FULLF",
			datoStatusEndring = endretDato,
			oppstartDato = endretDato.plusDays(1),
			sluttDato = endretDato.plusDays (2)
		)
		resultat shouldBe endretDato
	}

	@Test
	fun `convert() - avsluttende status - endretdato mellom start og sluttdato - returnerer sluttdato` () {
		val sluttdato = endretDato.plusDays(1)
		val resultat = converter.convert(
			deltakerStatus = "FULLF",
			datoStatusEndring = endretDato,
			oppstartDato = endretDato.minusDays(1),
			sluttDato = sluttdato
		)
		resultat shouldBe sluttdato
	}

	@Test
	fun `convert() - avsluttende status - endretdato etter start og sluttdato - returnerer endretdato` () {

		val resultat = converter.convert(
			deltakerStatus = "FULLF",
			datoStatusEndring = endretDato,
			oppstartDato = endretDato.minusDays(2),
			sluttDato = endretDato.minusDays(1)
		)
		resultat shouldBe endretDato
	}

	@Test
	fun `convert() - avsluttende status - endretdato etter start og sluttdato mangler - returnerer endretdato` () {

		val resultat = converter.convert(
			deltakerStatus = "FULLF",
			datoStatusEndring = endretDato,
			oppstartDato = endretDato.minusDays(2),
			sluttDato = null
		)
		resultat shouldBe endretDato
	}

	@Test
	fun `convert() - gjennomførende status - startdato har ikke passert - returnerer endretdato` () {

		val resultat = converter.convert(
			deltakerStatus = "GJENN",
			datoStatusEndring = endretDato,
			oppstartDato = endretDato.plusDays(1),
			sluttDato = endretDato.plusDays(2)
		)
		resultat shouldBe endretDato
	}

	@Test
	fun `convert() - gjennomførende status - mellom start og sluttdato - returnerer startdato` () {
		val oppstartDato = endretDato.minusDays(1)
		val resultat = converter.convert(
			deltakerStatus = "GJENN",
			datoStatusEndring = endretDato,
			oppstartDato = oppstartDato,
			sluttDato = endretDato.plusDays(2)
		)
		resultat shouldBe oppstartDato
	}

	@Test
	fun `convert() - gjennomførende status - startdato og sluttdato har passert - returnerer sluttdato` () {
		val sluttDato = endretDato.minusDays(1)
		val resultat = converter.convert(
			deltakerStatus = "GJENN",
			datoStatusEndring = endretDato,
			oppstartDato = endretDato.minusDays(2),
			sluttDato = sluttDato
		)
		resultat shouldBe sluttDato
	}

	@Test
	fun `convert() - gjennomførende status - startdato passert og sluttdato mangler - returnerer oppstartDato` () {
		val oppstartDato = endretDato.minusDays(2)
		val resultat = converter.convert(
			deltakerStatus = "GJENN",
			datoStatusEndring = endretDato,
			oppstartDato = oppstartDato,
			sluttDato = null
		)
		resultat shouldBe oppstartDato
	}

	@Test
	fun `convert() - ikke aktuell status - mellom start og slutt - returnerer endretdato` () {
		val resultat = converter.convert(
			deltakerStatus = "IKKEAKTUELL",
			datoStatusEndring = endretDato,
			oppstartDato = endretDato.minusDays(1),
			sluttDato = endretDato.plusDays(2)
		)
		resultat shouldBe endretDato
	}

}

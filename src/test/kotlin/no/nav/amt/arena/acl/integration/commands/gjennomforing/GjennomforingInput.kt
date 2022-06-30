package no.nav.amt.arena.acl.integration.commands.gjennomforing

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

data class GjennomforingInput(
	val tiltakKode: String = "INDOPPFAG",
	val gjennomforingId: Long = Random().nextLong(),
	val arbeidsgiverIdArrangor: Long? = 0,
	val navn: String? = UUID.randomUUID().toString(),
	val startDato: LocalDate = LocalDate.now().minusDays(7),
	val sluttDato: LocalDate = LocalDate.now().plusDays(7),
	val fremmoteDato: LocalDateTime = LocalDateTime.now().minusDays(7),
	val registrertDato: LocalDateTime = LocalDateTime.now().minusDays(14).truncatedTo(ChronoUnit.HOURS),
	val tiltakStatusKode: String = "GJENNOMFOR",
	val sakId: Long = Random().nextLong()
)

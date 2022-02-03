package no.nav.amt.arena.acl.domain.amt

import no.nav.amt.arena.acl.utils.ObjectMapperFactory
import org.springframework.util.DigestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class AmtGjennomforing(
	val id: UUID,
	val tiltak: AmtTiltak,
	val virksomhetsnummer: String,
	val navn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val registrertDato: LocalDateTime,
	val fremmoteDato: LocalDateTime?,
	val status: Status
) {

	private val objectMapper = ObjectMapperFactory.get()

	enum class Status {
		IKKE_STARTET,
		GJENNOMFORES,
		AVSLUTTET
	}

	fun digest() = DigestUtils.md5DigestAsHex(objectMapper.writeValueAsString(this).toByteArray())
}

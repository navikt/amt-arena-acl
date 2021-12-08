package no.nav.amt.arena.acl.domain.amt

import no.nav.amt.arena.acl.domain.arena.ArenaTiltakDeltaker
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

enum class AmtDeltakerStatus {
	WOLOLO
}

data class AmtDeltaker(
	val id: UUID,
	val gjennomforingId: UUID,
	val personIdent: String,
	val oppstartDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: AmtDeltakerStatus,
	val dagerPerUke: Int?,
	val prosentDeltid: Float?
) : AmtPayload

class AmtDeltakerMapper {
	companion object {
		fun map(
			id: UUID = UUID.randomUUID(),
			gjennomforingId: UUID,
			personligIdent: String,
			arenaTiltakDeltaker: ArenaTiltakDeltaker
		): AmtDeltaker {
			return AmtDeltaker(
				id = id,
				gjennomforingId = gjennomforingId,
				personIdent = personligIdent,
				oppstartDato = arenaTiltakDeltaker.DATO_FRA?.asLocalDate(),
				sluttDato = arenaTiltakDeltaker.DATO_TIL?.asLocalDate(),
				status = mapStatus(arenaTiltakDeltaker.DELTAKERSTATUSKODE),
				dagerPerUke = arenaTiltakDeltaker.ANTALL_DAGER_PR_UKE,
				prosentDeltid = arenaTiltakDeltaker.PROSENT_DELTID
			)
		}

		private fun mapStatus(arenaStatus: String): AmtDeltakerStatus {
			return AmtDeltakerStatus.WOLOLO
		}
	}
}

fun String.asLocalDate(): LocalDate {
	val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
	return LocalDate.parse(this, formatter)
}

fun String.asLocalDateTime(): LocalDateTime {
	val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
	return LocalDateTime.parse(this, formatter)
}

fun String?.asTime(): LocalTime {
	val logger = LoggerFactory.getLogger(String::class.java)

	if (this == null) {
		return LocalTime.MIDNIGHT
	} else if (this.matches("\\d\\d:\\d\\d".toRegex())) {
		val split = this.split(":")
		return LocalTime.of(split[0].toInt(), split[1].toInt())
	} else if (this.matches("\\d\\d\\.\\d\\d".toRegex())) {
		val split = this.split(".")
		return LocalTime.of(split[0].toInt(), split[1].toInt())
	} else if (this.matches("\\d\\d\\d\\d".toRegex())) {
		val hour = this.substring(0, 2)
		val minutes = this.substring(2, 4)

		return LocalTime.of(hour.toInt(), minutes.toInt())
	}

	if (this != null) logger.warn("Det er ikke implementert en handler for klokketid, pattern: $this")
	return LocalTime.MIDNIGHT
}

infix fun LocalDate?.withTime(time: LocalTime) =
	if (this != null) LocalDateTime.of(this, time) else null

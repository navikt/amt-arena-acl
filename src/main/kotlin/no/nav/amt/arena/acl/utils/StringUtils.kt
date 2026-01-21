package no.nav.amt.arena.acl.utils

import no.nav.amt.arena.acl.exceptions.ValidationException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun String.asValidatedLocalDate(fieldName: String): LocalDate {
	try {
		return this.asLocalDate()
	} catch (_: DateTimeParseException) {
		throw ValidationException("$fieldName kan ikke parses til LocalDate ($this)")
	}
}

fun String.asValidatedLocalDateTime(fieldName: String): LocalDateTime {
	try {
		return this.asLocalDateTime()
	} catch (_: DateTimeParseException) {
		throw ValidationException("$fieldName kan ikke parses til LocalDateTime ($this)")
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

fun String.removeNullCharacters(): String =
	this
		.replace("\u0000", "")
		.replace("\\u0000", "")

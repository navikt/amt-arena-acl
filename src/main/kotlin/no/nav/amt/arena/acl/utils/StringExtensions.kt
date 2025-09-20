package no.nav.amt.arena.acl.utils

import no.nav.amt.arena.acl.exceptions.ValidationException
import no.nav.amt.arena.acl.utils.DateUtils.localDateTimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime

fun String.asValidatedLocalDate(fieldName: String): LocalDate =
	runCatching {
		this.asLocalDate()
	}.getOrElse {
		throw ValidationException("$fieldName kan ikke parses til LocalDate ($this)")
	}

fun String.asValidatedLocalDateTime(fieldName: String): LocalDateTime =
	runCatching {
		this.asLocalDateTime()
	}.getOrElse {
		throw ValidationException("$fieldName kan ikke parses til LocalDateTime ($this)")
	}

fun String.asLocalDate(): LocalDate =
	LocalDate.parse(this, localDateTimeFormatter)

fun String.asLocalDateTime(): LocalDateTime =
	LocalDateTime.parse(this, localDateTimeFormatter)

fun String.removeNullCharacters(): String = this
	.replace("\u0000", "")
	.replace("\\u0000", "")

package no.nav.amt.arena.acl.utils

import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

fun ResultSet.getNullableString(columnLabel: String): String? = this.getString(columnLabel)

fun ResultSet.getUUID(columnLabel: String): UUID =
	getNullableUUID(columnLabel) ?: throw IllegalStateException("Expected $columnLabel not to be null")

fun ResultSet.getNullableUUID(columnLabel: String): UUID? =
	this
		.getString(columnLabel)
		?.let { UUID.fromString(it) }

fun ResultSet.getLocalDateTime(columnLabel: String): LocalDateTime =
	getNullableLocalDateTime(columnLabel) ?: throw IllegalStateException("Expected $columnLabel not to be null")

fun ResultSet.getNullableLocalDateTime(columnLabel: String): LocalDateTime? = this.getTimestamp(columnLabel)?.toLocalDateTime()

fun ResultSet.getLocalDate(columnLabel: String): LocalDate =
	getNullableLocalDate(columnLabel) ?: throw IllegalStateException("Expected $columnLabel not to be null")

fun ResultSet.getNullableLocalDate(columnLabel: String): LocalDate? = this.getDate(columnLabel)?.toLocalDate()

fun ResultSet.getNullableZonedDateTime(columnLabel: String): ZonedDateTime? {
	val timestamp = this.getTimestamp(columnLabel) ?: return null
	return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp.time), ZoneOffset.systemDefault())
}

fun ResultSet.getZonedDateTime(columnLabel: String): ZonedDateTime =
	getNullableZonedDateTime(columnLabel) ?: throw IllegalStateException("Expected $columnLabel not to be null")

fun ResultSet.getNullableInteger(columnLabel: String): Int? {
	val value = this.getInt(columnLabel)
	return if (this.wasNull()) null else value
}

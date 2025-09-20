package no.nav.amt.arena.acl.utils

import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun ResultSet.getUUID(columnLabel: String): UUID =
	getNullableUUID(columnLabel) ?: throw IllegalStateException("Expected $columnLabel not to be null")

fun ResultSet.getNullableUUID(columnLabel: String): UUID? =
	this.getString(columnLabel)?.let { UUID.fromString(it) }

fun ResultSet.getLocalDateTime(columnLabel: String): LocalDateTime =
	getNullableLocalDateTime(columnLabel) ?: throw IllegalStateException("Expected $columnLabel not to be null")

fun ResultSet.getNullableLocalDateTime(columnLabel: String): LocalDateTime? =
	getTimestamp(columnLabel)?.toLocalDateTime()

fun ResultSet.getLocalDate(columnLabel: String): LocalDate =
	getNullableLocalDate(columnLabel) ?: throw IllegalStateException("Expected $columnLabel not to be null")

fun ResultSet.getNullableLocalDate(columnLabel: String): LocalDate? =
	getDate(columnLabel)?.toLocalDate()

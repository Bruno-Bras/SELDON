package com.seldon.fleet.presentation.common

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val SHORT_UTC = DateTimeFormatter.ofPattern("dd/MM · HH:mm 'UTC'", PT_BR).withZone(ZoneOffset.UTC)
private val LONG_UTC = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'", PT_BR).withZone(ZoneOffset.UTC)
private val DATE_UTC = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR).withZone(ZoneOffset.UTC)
private val TIME_UTC = DateTimeFormatter.ofPattern("HH:mm 'UTC'", PT_BR).withZone(ZoneOffset.UTC)

fun formatDecimal(value: Double?, digits: Int = 1, fallback: String = "N/D"): String {
    if (value == null || !value.isFinite()) return fallback
    return String.format(PT_BR, "%.${digits}f", value)
}

fun formatPercent(value: Double?, digits: Int = 1, fallback: String = "N/D"): String =
    value?.let { "${formatDecimal(it, digits)}%" } ?: fallback

fun formatInteger(value: Int?, fallback: String = "N/D"): String = value?.toString() ?: fallback

fun formatUtcShort(epochMs: Long?): String =
    epochMs?.let { SHORT_UTC.format(Instant.ofEpochMilli(it)) } ?: "SEM SYNC"

fun formatUtcLong(epochMs: Long?): String =
    epochMs?.let { LONG_UTC.format(Instant.ofEpochMilli(it)) } ?: "SEM CONTATO REGISTRADO"

fun formatUtcDate(epochMs: Long?): String =
    epochMs?.let { DATE_UTC.format(Instant.ofEpochMilli(it)) } ?: "N/D"

fun formatUtcTime(epochMs: Long?): String =
    epochMs?.let { TIME_UTC.format(Instant.ofEpochMilli(it)) } ?: "N/D"

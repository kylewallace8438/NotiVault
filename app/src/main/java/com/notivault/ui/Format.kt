package com.notivault.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val timeOnly = DateTimeFormatter.ofPattern("HH:mm")
    private val dayTime = DateTimeFormatter.ofPattern("d MMM, HH:mm")
    private val full = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
    private val fullSeconds = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss")

    /** "14:05" today, "3 Oct, 14:05" this year, "3 Oct 2025, 14:05" otherwise. */
    fun short(ms: Long): String {
        val zone = ZoneId.systemDefault()
        val t = Instant.ofEpochMilli(ms).atZone(zone)
        val now = ZonedDateTime.now(zone)
        return when {
            t.toLocalDate() == now.toLocalDate() -> timeOnly.format(t)
            t.year == now.year -> dayTime.format(t)
            else -> full.format(t)
        }
    }

    fun long(ms: Long): String = fullSeconds.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    fun number(value: Double): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }.format(value)

    fun money(amount: Double, currency: String?, direction: String?): String {
        val sign = when (direction) {
            "IN" -> "+"
            "OUT" -> "−"
            else -> ""
        }
        return buildString {
            append(sign)
            append(number(amount))
            if (!currency.isNullOrBlank()) append(' ').append(currency)
        }
    }
}

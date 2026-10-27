package com.rupayonhaldar.gtafreestem.ui.format

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Presentation only: timed GTA events use Toronto's actual seasonal offset. */
internal object OpportunitySchedule {
    private val zone = ZoneId.of("America/Toronto")
    private fun instant(value: String?) = value?.trim()?.let { raw ->
        runCatching { Instant.parse(raw) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
    }
    private fun day(value: String?) = value?.trim()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun dateTime(value: String?, locale: Locale): String? {
        val raw = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        day(raw)?.let { return it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) }
        instant(raw)?.atZone(zone)?.let {
            return it.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)) +
                " " + it.format(DateTimeFormatter.ofPattern("z", locale))
        }
        return raw
    }

    fun range(start: String?, end: String?, locale: Locale): String? {
        val first = dateTime(start, locale)
        val last = dateTime(end, locale)
        if (first == null) return last
        if (last == null || first == last) return first
        val a = instant(start)?.atZone(zone)
        val b = instant(end)?.atZone(zone)
        if (a != null && b != null && a.toLocalDate() == b.toLocalDate() && a.offset == b.offset) {
            val date = a.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
            val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            return "$date · ${a.format(time)}–${b.format(time)} ${b.format(DateTimeFormatter.ofPattern("z", locale))}"
        }
        return "$first – $last"
    }

    fun hasDistinctDeadline(start: String?, deadline: String?): Boolean {
        if (deadline.isNullOrBlank()) return false
        val startTime = instant(start)
        val deadlineTime = instant(deadline)
        if (startTime != null && deadlineTime != null) return startTime != deadlineTime
        val startDay = day(start)
        val deadlineDay = day(deadline)
        return startDay == null || deadlineDay == null || startDay != deadlineDay
    }
}

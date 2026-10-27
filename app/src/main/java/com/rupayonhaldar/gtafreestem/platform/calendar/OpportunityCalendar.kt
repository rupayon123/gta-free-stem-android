package com.rupayonhaldar.gtafreestem.platform.calendar

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** A user-reviewed platform form, never a direct calendar database write. */
internal object OpportunityCalendar {
    fun intent(opportunity: Opportunity, title: String, location: String, description: String = opportunity.description): Intent =
        Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                listOf(description, opportunity.sourceUrl).joinToString("\n\n"),
            )
            val startDay = opportunity.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val startTime = opportunity.startDate?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
            when {
                startDay != null -> {
                    putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDay.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
                    val endDay = opportunity.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    if (endDay != null && !endDay.isBefore(startDay)) {
                        // Calendar all-day ends are exclusive; feed date-only ends are inclusive.
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDay.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
                    }
                }
                startTime != null -> {
                    putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime.toInstant().toEpochMilli())
                    val endTime = opportunity.endDate?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
                    if (endTime != null && endTime.isAfter(startTime)) {
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.toInstant().toEpochMilli())
                    }
                }
                // An absent schedule stays absent. The person chooses dates in their calendar.
            }
        }

    fun open(context: Context, opportunity: Opportunity, title: String, location: String, description: String = opportunity.description): Boolean = try {
        context.startActivity(intent(opportunity, title, location, description))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

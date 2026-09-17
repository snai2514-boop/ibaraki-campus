package com.tyust.course.academic

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class SchoolWeekCourse(val description: String, val day: Int, val period: Int, val weeks: String, val location: String = "")

/** Converts dated school events to the existing weekly grid, using actual dates for replacement days. */
class SchoolTermView(val quarter: Int, val events: List<SchoolCalendarEvent>, val academicYear: Int = 2026, val anchorMonday: String? = null, val venue: (SchoolCalendarEvent) -> String = { "" }) {
    init { require(quarter in 1..4) }
    // Unverified years use calendar quarters for browsing only; school events are never extrapolated.
    val firstMonday = anchorMonday ?: if (academicYear == 2026) listOf("2026-04-06", "2026-06-01", "2026-09-21", "2026-11-23")[quarter - 1]
        else format(Calendar.getInstance(IbarakiTimetable.zone).apply {
            clear(); set(academicYear, listOf(3, 5, 8, 10)[quarter - 1], 1)
            add(Calendar.DAY_OF_MONTH, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
        })
    // Include Q1's June 9 reserve day and all dates before the August 12 summer break.
    // Whole-week rendering also shows the start of the holiday with its own label.
    val weekCount = if (academicYear == 2026 && anchorMonday == null) when (quarter) {
        1 -> 10
        2 -> 11
        4 -> 14
        else -> 10
    } else if (quarter <= 2) 9 else 10
    fun date(week: Int, day: Int): String {
        require(week in 1..weekCount && day in 1..7)
        return format(IbarakiTimetable.parseDate(firstMonday)!!.apply { add(Calendar.DAY_OF_MONTH, (week - 1) * 7 + day - 1) })
    }
    fun week(date: String): Int = Math.floorDiv(IbarakiTimetable.parseDate(date)!!.timeInMillis - IbarakiTimetable.parseDate(firstMonday)!!.timeInMillis, 7 * 86400000L).toInt() + 1
    val courses: List<SchoolWeekCourse> get() = events.filter { it.date in date(1, 1)..date(weekCount, 7) }.groupBy {
        val actualDay = (IbarakiTimetable.parseDate(it.date)!!.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        listOf(it.lesson.description, actualDay.toString(), it.lesson.period.toString(), venue(it))
    }.map { (key, rows) -> SchoolWeekCourse(key[0], key[1].toInt(), key[2].toInt(), rows.map { week(it.date) }.distinct().sorted().joinToString(","), key[3]) }
    companion object {
        fun format(date: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = IbarakiTimetable.zone }.format(date.time)
    }
}

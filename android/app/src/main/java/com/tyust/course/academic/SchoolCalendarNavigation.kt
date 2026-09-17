package com.tyust.course.academic

import java.util.Calendar

/** Navigation metadata only: never creates school events for unpublished years. */
object SchoolCalendarNavigation {
    fun termKey(date: String, ownerPrefix: String): String {
        val day=requireNotNull(IbarakiTimetable.parseDate(date))
        val calendarYear=day.get(Calendar.YEAR)
        val candidates=(calendarYear-1..calendarYear+1).flatMap {year -> (1..4).map {quarter ->
            Triple(year,quarter,SchoolTermView(quarter,emptyList(),year).firstMonday)
        } }
        val (year,quarter)=candidates.filter {it.third<=date}.maxBy {it.third}
        return "${ownerPrefix}timetable-$year-Q$quarter"
    }
    fun moveWeek(date: String, delta: Int): String {
        val day=requireNotNull(IbarakiTimetable.parseDate(date))
        day.add(Calendar.DAY_OF_MONTH,-((day.get(Calendar.DAY_OF_WEEK)+5)%7)+delta*7)
        return SchoolTermView.format(day)
    }
    data class Source(val key: String, val event: SchoolCalendarEvent)
    /** Callers supply only the current student's snapshots, newest first. */
    fun merge(sources: List<Source>): List<Source> = sources.distinctBy {
        listOf(it.event.date,it.event.lesson.period.toString(),it.event.lesson.description.substringBefore(' '))
    }.sortedWith(compareBy({it.event.date},{it.event.lesson.period}))
}

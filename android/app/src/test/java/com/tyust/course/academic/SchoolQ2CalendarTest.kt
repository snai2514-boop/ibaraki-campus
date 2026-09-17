package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolQ2CalendarTest {
    private fun snapshot(vararg lessons: PortalLesson) = PortalImport("account/timetable-2026-Q2", "Q2", emptyList(), lessons = lessons.toList())
    @Test fun tuesdaySemesterAndQuarterHaveDifferentDates() {
        val q = SchoolQ2Calendar.events(snapshot(PortalLesson(2, 1, "KB1000 講義【2Q】 教員 1.0単位")))
        val s = SchoolQ2Calendar.events(snapshot(PortalLesson(2, 3, "T5008 講義 教員 2.0単位")))
        assertEquals("2026-06-16", q.first().date); assertEquals("2026-07-28", q.last().date)
        assertEquals("2026-06-09", s.first().date); assertEquals("2026-07-21", s.last().date)
        assertEquals(7, s.size); assertEquals(7, q.size)
    }
    @Test fun holidayAndReserveDaysAreExcluded() {
        val events = SchoolQ2Calendar.events(snapshot(PortalLesson(1, 1, "KB1000 講義【2Q】 教員 1.0単位")))
        assertFalse(events.any { it.date in listOf("2026-07-20", "2026-07-30", "2026-06-06") })
        assertEquals("2026-07-27", events.last().date)
        assertTrue(events.last().examPossible)
    }
    @Test fun unknownYearOrCourseCannotProduceInventedDates() {
        val item = snapshot(PortalLesson(1, 1, "UNKNOWN 講義"))
        assertTrue(runCatching { SchoolQ2Calendar.events(item) }.isFailure)
        assertTrue(runCatching { SchoolQ2Calendar.events(item.copy(key = "account/timetable-2026-Q3")) }.isFailure)
    }
    @Test fun utcTimesAndUtf8FoldingAreValid() {
        val item = snapshot(PortalLesson(1, 1, "KB1000 ${"講義".repeat(60)}【2Q】 教員 1.0単位"))
        val ics = SchoolQ2Calendar.ics(item)
        assertTrue(ics.contains("DTSTART:20260607T234000Z"))
        assertTrue(ics.contains("DTEND:20260608T012500Z"))
        assertEquals(7, Regex("BEGIN:VEVENT").findAll(ics).count())
        assertTrue(ics.split("\r\n").all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        assertEquals(Regex("UID:.*").findAll(ics).map { it.value }.toList(), Regex("UID:.*").findAll(SchoolQ2Calendar.ics(item)).map { it.value }.toList())
    }
}

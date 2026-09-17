package com.tyust.course.academic

import androidx.compose.ui.graphics.Color
import com.tyust.course.schedule.ScheduleCourseRecord
import com.tyust.course.schedule.ScheduleDates
import com.tyust.course.schedule.scheduleConflicts
import com.tyust.course.ui.screen.ScheduleCourseUi
import com.tyust.course.utils.ICalExporter
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class IbarakiTimetableTest {
    private val course = ScheduleCourseRecord("stable", "日本語,演習", "先生", "教室;A", 1, 1, 1, "1,3", true)
    private val timetable = IbarakiTimetable("2026-09-07", listOf(course))
    @Test fun jsonRoundTripPreservesDatesJapaneseAndIdentity() {
        assertEquals(timetable, IbarakiTimetableCodec.decode(IbarakiTimetableCodec.encode(timetable)))
    }
    @Test fun selectedDatesRespectWeekGaps() {
        assertEquals(listOf(course), timetable.onDate("2026-09-07"))
        assertTrue(timetable.onDate("2026-09-14").isEmpty())
        assertEquals(listOf(course), timetable.onDate("2026-09-21"))
        assertTrue(timetable.onDate("2026-09-06").isEmpty())
        assertTrue(timetable.onDate("2026-09-08").isEmpty())
    }
    @Test fun japanDatesDoNotDependOnDeviceTimeZone() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(listOf(course), timetable.onDate("2026-09-07"))
        } finally { TimeZone.setDefault(previous) }
    }
    @Test fun exportUsesTokyoOffsetAndOfficialFirstPeriod() {
        val ui = ScheduleCourseUi(course.name, course.teacher, course.location, 1, 1, 1, course.weeks, Color.Blue, id = course.id)
        val text = ICalExporter.generateICalContent(listOf(ui), ScheduleDates.firstMonday(timetable.firstMonday, IbarakiTimetable.zone)!!,
            25, IbarakiTimetable.periodTimes, "Asia/Tokyo")
        assertTrue(text.contains("TZID:Asia/Tokyo"))
        assertTrue(text.contains("TZOFFSETTO:+0900"))
        assertTrue(text.contains("DTSTART;TZID=Asia/Tokyo:20260907T084000"))
        assertTrue(text.contains("DTEND;TZID=Asia/Tokyo:20260907T102500"))
        assertFalse(text.contains("Asia/Shanghai"))
        assertEquals(2, text.lineSequence().count { it == "BEGIN:VEVENT" })
    }
    @Test fun conflictsRequireOverlappingWeeksAndPeriods() {
        assertEquals(1, scheduleConflicts(course, listOf(course, course.copy(id = "other", weeks = "3"))).size)
        assertTrue(scheduleConflicts(course, listOf(course, course.copy(id = "other", weeks = "2"))).isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun invalidFirstMondayIsRejected() { timetable.copy(firstMonday = "2026-09-08").validate() }
    @Test(expected = IllegalArgumentException::class) fun nonJapanesePeriodIsRejected() { timetable.copy(courses = listOf(course.copy(endPeriod = 6))).validate() }
    @Test(expected = IllegalArgumentException::class) fun corruptStoredVersionIsRejected() { IbarakiTimetableCodec.decode("{\"version\":2}") }
    @Test fun invalidDateDoesNotRollOverIntoNextMonth() { assertNull(IbarakiTimetable.parseDate("2026-02-30")) }
}

package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class PersonalCalendarTest {
    @Test fun weeklyEntriesRespectWeekdayAndInclusiveEndAcrossYear() {
        val e = PersonalCalendarEntry(title = "自习", kind = "事件", date = "2026-12-30", endDate = "2027-01-13", weekly = true)
        e.validate()
        assertTrue(e.occursOn("2027-01-06")); assertTrue(e.occursOn("2027-01-13"))
        assertFalse(e.occursOn("2027-01-07")); assertFalse(e.occursOn("2027-01-20")); assertFalse(e.occursOn("2026-12-23"))
    }
    @Test fun singleEventsDoNotRepeat() {
        val e = PersonalCalendarEntry(title = "报告", kind = "事件", date = "2026-06-15", allDay = true)
        e.validate(); assertTrue(e.occursOn("2026-06-15")); assertFalse(e.occursOn("2026-06-22"))
    }
    @Test fun invalidDatesAndTimesCannotBeSaved() {
        val base = PersonalCalendarEntry(title = "课程", kind = "课程", date = "2026-06-15")
        listOf(base.copy(date = "2026-02-30"), base.copy(start = "25:00"), base.copy(end = "08:40"),
            base.copy(title = " "), base.copy(weekly = true, endDate = "2026-06-14")).forEach {
            assertTrue(runCatching { it.validate() }.isFailure)
        }
    }
    @Test fun conditionalAndMixedCoursesNeverBecomeOnlineOnly() {
        val key = "owner/timetable-2026-Q2"
        assertEquals("共通 21", SchoolCourseVenue.label(key, "KB4012 情報リテラシー", "共通 21"))
        assertEquals("共通 10", SchoolCourseVenue.label(key, "KB2004 茨城学", "共通 10"))
        assertEquals("线上上课", SchoolCourseVenue.label(key, "KB9307 思想・文学", "共通 10"))
        assertEquals("教室待确认", SchoolCourseVenue.label("timetable-2027-Q2", "KB9307 思想・文学"))
        assertEquals("教室待确认", SchoolCourseVenue.label(key, "UNKNOWN オンラインについて"))
    }
}

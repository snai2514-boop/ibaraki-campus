package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolLiveCalendarTest {
    @Test fun timetableInstructorSuffixDoesNotPreventExactCalendarNameMatch() {
        assertTrue(SchoolLiveCalendar.matchesName("KB3241 Integrated English 2C【後期】 Teacher Name 1.0単位", "Integrated English 2C【後期】"))
        assertFalse(SchoolLiveCalendar.matchesName("KB3241 Integrated English 2C【後期】 Teacher Name 1.0単位", "Integrated English"))
        assertFalse(SchoolLiveCalendar.matchesName("KB3241 Integrated English 2C【後期】 Teacher Name 1.0単位", "Integrated English 2D【後期】"))
        assertFalse(SchoolLiveCalendar.matchesName("KB9307 共生【3Q】表現行動 Teacher 1.0単位", "共生【3Q】表現"))
    }
    private fun snapshot(year: Int, q: Int, code: String = "NEW999") = PortalImport("owner/timetable-$year-Q$q", "", emptyList(),
        lessons = listOf(PortalLesson(5, 1, "$code New Course【後期】 2.0単位")))

    @Test fun unpublishedThenPublishedMeetingsUseActualDatesWithout2026Templates() {
        val saved = listOf(snapshot(2027, 3), snapshot(2027, 4))
        assertTrue(SchoolLiveCalendar.sources("owner", saved, emptyList()).isEmpty())
        val meetings = listOf(SchoolLiveMeeting("New Course【後期】", "2027-11-30", 1))
        val source = SchoolLiveCalendar.sources("owner", saved, meetings).single()
        assertEquals("2027-11-30", source.event.date)
        assertEquals(2, source.event.lesson.day) // actual Tuesday, not the template's Friday
        assertEquals("owner/timetable-2027-calendar", source.key)
        assertTrue(SchoolLiveCalendar.sources("other", saved, meetings).isEmpty())
        assertTrue(SchoolLiveCalendar.sources("owner", listOf(snapshot(2026, 3)), meetings).isEmpty())
    }
    @Test fun januaryBelongsToPreviousAcademicYearAndAmbiguousNamesAreRejected() {
        val meeting = listOf(SchoolLiveMeeting("New Course【後期】", "2028-01-14", 1))
        assertEquals(1, SchoolLiveCalendar.sources("owner", listOf(snapshot(2027, 4)), meeting).size)
        assertTrue(SchoolLiveCalendar.sources("owner", listOf(snapshot(2027, 4), snapshot(2027, 4, "OTHER999")), meeting).isEmpty())
    }
    @Test fun nextYearsFirstQuarterAlsoUsesSchoolDates() {
        val item = snapshot(2027, 1).copy(lessons=listOf(PortalLesson(1,1,"NEW999 New Course【1Q】 2.0単位")))
        val result = SchoolLiveCalendar.sources("owner", listOf(item), listOf(SchoolLiveMeeting("New Course【1Q】", "2027-04-12",1)))
        assertEquals("2027-04-12", result.single().event.date)
    }
}

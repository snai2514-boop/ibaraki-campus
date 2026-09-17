package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolCalendarNavigationTest {
    @Test fun chosenSeptemberDateMovesQ2ToQ3WithoutManualTermChange() {
        assertEquals("owner/timetable-2026-Q2",SchoolCalendarNavigation.termKey("2026-09-16","owner/"))
        assertEquals("owner/timetable-2026-Q3",SchoolCalendarNavigation.termKey("2026-09-25","owner/"))
        assertEquals("owner/timetable-2026-Q3",SchoolCalendarNavigation.termKey("2026-09-21","owner/"))
    }
    @Test fun previousNextWeeksCrossTermsAndYearsKeepingDates() {
        val next=SchoolCalendarNavigation.moveWeek("2026-09-16",1)
        assertEquals("2026-09-21",next)
        assertEquals("timetable-2026-Q3",SchoolCalendarNavigation.termKey(next,""))
        assertEquals("2026-09-14",SchoolCalendarNavigation.moveWeek(next,-1))
        assertEquals("2027-01-04",SchoolCalendarNavigation.moveWeek("2026-12-28",1))
        assertEquals("timetable-2026-Q4",SchoolCalendarNavigation.termKey("2027-01-04",""))
    }
    @Test fun manualTermAnchorsRoundTripAcrossAcademicYears() {
        for(year in 2025..2028) for(q in 1..4) {
            val first=SchoolTermView(q,emptyList(),year).firstMonday
            assertEquals("timetable-$year-Q$q",SchoolCalendarNavigation.termKey(first,""))
        }
    }
    @Test fun boundaryWeekIncludesBothQuartersAndMonthIncludesAllDays() {
        val q3=SchoolCalendarEvent("2026-11-24",PortalLesson(2,1,"KB9990 Course【後期】"),false)
        val q4=SchoolCalendarEvent("2026-11-25",PortalLesson(3,2,"KB9991 Course【4Q】"),false)
        val merged=SchoolCalendarNavigation.merge(listOf(
            SchoolCalendarNavigation.Source("timetable-2026-Q3",q3),SchoolCalendarNavigation.Source("timetable-2026-Q4",q4)))
        val model=SchoolTermView(4,merged.map {it.event})
        assertEquals("2026-11-23",model.date(1,1))
        assertEquals(2,model.courses.count {it.weeks=="1"})
        assertEquals(2,merged.count {it.event.date.startsWith("2026-11")})
        assertEquals("timetable-2026-Q3",merged.first().key)
    }
    @Test fun duplicateQuarterSnapshotsNeverDuplicateOneOccurrence() {
        val event=SchoolCalendarEvent("2026-06-09",PortalLesson(2,1,"KB9990 Course【前期】"),false)
        val newest=SchoolCalendarNavigation.Source("new",event)
        assertEquals(listOf(newest),SchoolCalendarNavigation.merge(listOf(newest,SchoolCalendarNavigation.Source("old",event))))
    }
    @Test fun selectingAnUnknownYearDoesNotCreateEvents() {
        assertEquals("timetable-2028-Q3",SchoolCalendarNavigation.termKey("2028-10-01",""))
        assertTrue(SchoolCalendarNavigation.merge(emptyList()).isEmpty())
    }
}

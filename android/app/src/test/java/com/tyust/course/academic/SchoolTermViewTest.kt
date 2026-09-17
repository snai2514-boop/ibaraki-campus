package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolTermViewTest {
    @Test fun firstSemesterIncludesReserveDaysAndAllDatesBeforeSummer() {
        val q1 = SchoolTermView(1, emptyList())
        val q2 = SchoolTermView(2, emptyList())
        assertTrue(q1.week("2026-06-09") in 1..q1.weekCount)
        assertTrue(q2.week("2026-08-11") in 1..q2.weekCount)
        assertEquals("2026-08-16", q2.date(q2.weekCount, 7))
        assertEquals("季度课程预备日", SchoolReserveDays.on("2026-06-09"))
        assertEquals("学期课程预备日", SchoolReserveDays.on("2026-07-28"))
        assertEquals("课程预备日", SchoolReserveDays.on("2026-07-31"))
        assertNull(SchoolReserveDays.on("2026-08-11"))
        assertNull(SchoolReserveDays.on("2025-07-31"))
        assertEquals("暑假", SchoolHolidays.on("2026-08-12")!!.name)
        assertTrue(q2.courses.isEmpty())
    }
    @Test fun replacementWednesdayAppearsInActualFridayColumn() {
        val lesson = PortalLesson(3, 2, "KB1000 講義【1Q】 1.0単位")
        val events = listOf(SchoolCalendarEvent("2026-04-15", lesson, false), SchoolCalendarEvent("2026-05-01", lesson, false))
        val model = SchoolTermView(1, events)
        assertEquals("2", model.courses.single { it.day == 3 }.weeks)
        assertEquals("4", model.courses.single { it.day == 5 }.weeks)
        assertEquals("2026-05-01", model.date(4, 5))
    }

    @Test fun weeksIncludeEmptyHolidayWeeksAndCrossYearDates() {
        val model = SchoolTermView(4, emptyList())
        assertEquals(14, model.weekCount)
        assertEquals("2027-01-25", model.date(10, 1))
        assertEquals(7, model.week("2027-01-06"))
        assertTrue(model.courses.isEmpty())
    }
    @Test fun fullAcademicYearReserveDaysIncludeWinterAndBeforeSpring() {
        val q3 = SchoolTermView(3, emptyList())
        val q4 = SchoolTermView(4, emptyList())
        assertTrue(q3.week("2026-11-28") in 1..q3.weekCount)
        assertEquals("季度课程预备日", SchoolReserveDays.on("2026-11-28"))
        assertEquals("学期课程预备日", SchoolReserveDays.on("2027-01-20"))
        assertEquals("课程预备日", SchoolReserveDays.on("2027-01-30"))
        assertEquals("季度课程预备日", SchoolReserveDays.on("2027-02-01"))
        assertTrue(q4.week("2027-02-23") in 1..q4.weekCount)
        assertEquals("2027-02-28", q4.date(q4.weekCount, 7))
        assertEquals("寒假", SchoolHolidays.on("2027-01-05")!!.name)
        assertEquals("春假", SchoolHolidays.on("2027-02-24")!!.name)
        assertNull(SchoolReserveDays.on("2027-01-05"))
        assertNull(SchoolReserveDays.on("2027-02-23"))
        assertTrue(q4.courses.isEmpty())
    }

    @Test fun repeatOccurrencesBecomeWeeksWithoutDuplicateGridCards() {
        val lesson = PortalLesson(2, 1, "KB1000 講義【2Q】 1.0単位")
        val events = listOf("2026-06-16", "2026-06-23", "2026-07-28").map { SchoolCalendarEvent(it, lesson, false) }
        val model = SchoolTermView(2, events + events.first())
        assertEquals(1, model.courses.size)
        assertEquals("3,4,9", model.courses.single().weeks)
        assertEquals("2026-08-02", model.date(9, 7))
    }
    @Test fun otherAcademicYearsDoNotReuse2026DatesOrInventCourses() {
        for (year in 2024..2029) if (year != 2026) {
            for (quarter in 1..4) {
                val model = SchoolTermView(quarter, emptyList(), year)
                assertTrue(model.firstMonday.startsWith("$year-"))
                assertTrue(model.courses.isEmpty())
                assertEquals(1, model.week(model.firstMonday))
                assertEquals(java.util.Calendar.MONDAY, IbarakiTimetable.parseDate(model.firstMonday)!!.get(java.util.Calendar.DAY_OF_WEEK))
            }
        }
    }
    @Test fun summerWeekKeepsCurrentDatesAndDoesNotMoveOldCourses() {
        val lesson = PortalLesson(1, 1, "KB4012 Course")
        val model = SchoolTermView(2, listOf(SchoolCalendarEvent("2026-06-01", lesson, false)), 2026, "2026-09-14")
        assertEquals("2026-09-16", model.date(1, 3))
        assertEquals(1, model.week("2026-09-16"))
        assertEquals(0, model.week("2026-09-13"))
        assertTrue(model.courses.isEmpty())
    }
    @Test fun browsingWeekExcludesPastAndFutureOccurrencesOfSameCourse() {
        val lesson = PortalLesson(3, 1, "KB4012 Course")
        val events = listOf("2026-06-03", "2026-09-16", "2027-01-06").map { SchoolCalendarEvent(it, lesson, false) }
        val model = SchoolTermView(2, events, 2026, "2026-09-14")
        assertEquals("1", model.courses.single().weeks)
        assertEquals(3, model.courses.single().day)
        assertTrue(com.tyust.course.schedule.ScheduleWeeks.parse(model.courses.single().weeks).valid)
    }
}

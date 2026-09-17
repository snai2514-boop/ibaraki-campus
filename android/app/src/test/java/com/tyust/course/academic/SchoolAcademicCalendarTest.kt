package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolAcademicCalendarTest {
    private fun events(q: Int, day: Int, description: String) = SchoolAcademicCalendar.events(
        PortalImport("account/timetable-2026-Q$q", "", emptyList(), lessons = listOf(PortalLesson(day, 2, description))))

    @Test fun springReplacementDayAndQuarterExamArePreserved() {
        val result = events(1, 3, "KB1000 講義【1Q】 1.0単位")
        assertEquals(7, result.size)
        assertTrue(result.any { it.date == "2026-05-01" })
        assertFalse(result.any { it.date == "2026-04-29" })
        assertTrue(result.last().examPossible)
        assertFalse(events(1, 3, "T5005 演習 2.0単位").any { it.examPossible })
    }

    @Test fun fullSemesterHasFourteenDistinctDatesAcrossQuarterSlices() {
        for (day in 1..5) {
            for (startQuarter in listOf(1, 3)) {
                val label = if (startQuarter == 1) "前期" else "後期"
                val result = events(startQuarter, day, "KB1000 講義【$label】 2.0単位") +
                    events(startQuarter + 1, day, "KB1000 講義【$label】 2.0単位")
                assertEquals(14, result.size)
                assertEquals(14, result.map { it.date }.distinct().size)
                assertEquals(1, result.count { it.examPossible })
            }
        }
    }

    @Test fun winterReplacementAndCampusSpecificFridayAreCorrect() {
        val monday = events(4, 1, "KB1000 講義【4Q】 1.0単位")
        assertTrue(monday.any { it.date == "2027-01-06" })
        assertFalse(monday.any { it.date == "2027-01-11" })
        val friday = events(3, 5, "KB1000 講義【3Q】 1.0単位")
        assertTrue(friday.any { it.date == "2026-10-16" })
        assertFalse(friday.any { it.date == "2026-11-13" })
        assertEquals("2026-11-06", friday.last().date)
    }

    @Test fun unverifiedCampusOrConflictingTermDoesNotGenerateDates() {
        assertTrue(runCatching { events(3, 5, "T9999 不明【3Q】 1.0単位") }.isFailure)
        assertTrue(runCatching { events(1, 1, "T5001 不整合【1Q】 2.0単位") }.isFailure)
        assertEquals(7, events(1, 1, "T5003 化学概論 1.0単位").size)
    }

    @Test fun mechanicalAndOtherEngineeringFirstYearCoursesDoNotNeedInformationEngineeringCodes() {
        for (code in listOf("T1001", "T1011", "T1013", "T1069", "T1070", "T3002", "T4001", "T6001")) {
            assertEquals(code, 7, events(2, 2, "$code 講義 2.0単位").size)
            assertEquals(code, "2026-06-09", events(2, 2, "$code 講義 2.0単位").first().date)
            assertEquals(2, SchoolAcademicCalendar.creditQuarter(2026, code, "$code 講義 2.0単位"))
        }
    }

    @Test fun oneUnknownOrWeekendCourseDoesNotEraseVerifiedCourses() {
        val item = PortalImport("account/timetable-2026-Q2", "", emptyList(), lessons = listOf(
            PortalLesson(2, 4, "T1001 線形代数Ⅰ 2.0単位"),
            PortalLesson(3, 2, "UNKNOWN 講義 2.0単位"),
            PortalLesson(6, 2, "KB1000 週末【2Q】 1.0単位")))
        val result = SchoolAcademicCalendar.resolve(item)
        assertEquals(7, result.events.size)
        assertEquals(2, result.issues.size)
        assertTrue(runCatching { SchoolQ2Calendar.ics(item) }.isFailure)
    }

    @Test fun campusFridaysAndOtherFacultiesAreResolvedIndependently() {
        val hitachi = events(3, 5, "T1075 材料力学演習 1.0単位")
        assertTrue(hitachi.any { it.date == "2026-11-13" })
        assertFalse(hitachi.any { it.date == "2026-11-06" })
        val ami = events(3, 5, "AN9999 講義【3Q】 阿見 1.0単位")
        assertTrue(ami.any { it.date == "2026-11-13" })
        assertFalse(ami.any { it.date == "2026-10-23" })
        assertTrue(ami.any { it.date == "2026-10-16" })
        for (faculty in listOf("人文社会科学部", "教育学部", "理学部", "地域未来共創学環")) {
            val profile = SchoolStudentProfile("", "", faculty, "", "", "")
            val item = PortalImport("a/timetable-2026-Q3", "", emptyList(), lessons = listOf(PortalLesson(5,2,"X9000 講義【3Q】 1.0単位")))
            assertEquals("2026-11-06", SchoolAcademicCalendar.events(item, profile).last().date)
        }
        assertEquals(7, events(1, 2, "LA0001 メディア文化入門 2.0単位").size)
        assertEquals(6, events(4, 2, "P0502 代数学基礎 2.0単位").size)
    }

    @Test fun unknownFacultyStillUsesSharedDatesButNotGuessedCampusFriday() {
        assertEquals(7, events(2, 3, "AN9999 講義【2Q】 1.0単位").size)
        assertTrue(runCatching { events(3, 5, "AN9999 講義【3Q】 1.0単位") }.isFailure)
        assertTrue(runCatching { events(1, 1, "T9920 集中講義 2.0単位") }.isFailure)
    }

    @Test fun allRegularOfficialOfferingsGenerateDatesInTheirOwnTermAndCampus() {
        assertTrue(SchoolCourseOfferings2026.rules.size > 1800)
        for ((code, rule) in SchoolCourseOfferings2026.rules) {
            if (rule.irregular) continue
            val quarters = when(rule.term) { "前期" -> listOf(1,2); "後期" -> listOf(3,4); "通年" -> listOf(1,2,3,4); else -> listOf(rule.term.take(1).toInt()) }
            for (quarter in quarters) for (day in 1..5) {
                val result = events(quarter, day, "$code 講義 2.0単位")
                assertTrue("$code Q$quarter", result.isNotEmpty())
                assertEquals(result.size, result.map { it.date }.distinct().size)
            }
        }
        assertEquals(SchoolCampus.MITO, SchoolCourseOfferings2026.find("AN1203")!!.campus)
        assertEquals(SchoolCampus.AMI, SchoolCourseOfferings2026.find("AN2208")!!.campus)
        assertEquals(SchoolCourseOfferings2026.find("T1018-A"), SchoolCourseOfferings2026.find("T1018"))
    }

    @Test fun scienceUsesFullIdentityAndNeverAnotherFacultyOrWrongSlot() {
        val lesson = PortalLesson(1,1,"S9999 化学Ⅰ 2.0単位")
        val profile = SchoolStudentProfile("", "", "理学部", "", "", "")
        val item = PortalImport("a/timetable-2026-Q2", "", emptyList(), lessons = listOf(lesson))
        assertEquals(7, SchoolAcademicCalendar.events(item, profile).size)
        assertNotNull(SchoolScienceOfferings2026.find(lesson.copy(description="S9999 化学Ⅰ【理】 教員 2.0単位"),2,"理学部"))
        assertNull(SchoolScienceOfferings2026.find(lesson,2,"工学部"))
        assertNull(SchoolScienceOfferings2026.find(lesson.copy(day=2),2,"理学部"))
        assertNull(SchoolScienceOfferings2026.find(lesson.copy(description="S9999 化学Ⅰ 1.0単位"),2,"理学部"))
        val grade = PortalImport("a/grades", "", emptyList())
        assertEquals("预计新增", SchoolCreditForecast.calculate(grade,listOf(item),2026,setOf(2),"理学部").courses.single().state)
        assertEquals(7, events(1,2,"R2001 経済学概論 2.0単位").size)
    }
}

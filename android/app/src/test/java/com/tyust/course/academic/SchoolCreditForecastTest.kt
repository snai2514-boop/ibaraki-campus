package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SchoolCreditForecastTest {
    @Test fun otherDepartmentsUsePublishedResultsWithoutCodeWhitelist() {
        val result = SchoolCreditForecast.calculate(grades(g("線形代数Ⅰ", "2"), g("教育学概論", "2", false)),
            listOf(table(2, "T1001 線形代数Ⅰ【機械】 教師 2.0単位", "E1234 教育学概論【教育】 教師 2.0単位")), 2026, setOf(1, 2))
        assertEquals(listOf("不合格，不计新增", "已修得，不重复增加"), result.courses.map { it.state })
        assertEquals(0, BigDecimal("2").compareTo(result.completed))
        assertEquals(0, BigDecimal("2").compareTo(result.failed))
        assertEquals(0, result.added.signum())
        assertTrue(SchoolCreditForecast.calculate(grades(g("線形代数Ⅰ", "2")),
            listOf(table(2, "T1001 線形代数Ⅰ【機械】 教師 2.0単位")), 2026, setOf(3, 4)).courses.isEmpty())
    }
    @Test fun previousYearAndSecondSemesterResultsAreSupported() {
        val grade = grades(g("経済学", "2").copy(year = "2025", term = "後期"))
        val timetable = table(4, "H1234 経済学【人文】 教師 2.0単位").copy(key = "a/timetable-2025-Q4")
        val result = SchoolCreditForecast.calculate(grade, listOf(timetable), 2025, setOf(3, 4))
        assertEquals("已修得，不重复增加", result.courses.single().state)
        assertEquals(4, result.courses.single().quarter)
    }
    @Test fun pageQuarterDisambiguatesSameNameResultsButNeverUsesWrongYear() {
        val tables = listOf(table(2, "X1 共通科目【学部】 教師 2.0単位"))
        val ambiguous = grades(g("共通科目", "2", q=1), g("共通科目", "2", q=2))
        val result = SchoolCreditForecast.calculate(ambiguous, tables, 2026, setOf(1,2))
        assertEquals("已修得，不重复增加", result.courses.single().state)
        assertEquals(2, result.courses.single().quarter)
        val old = grades(g("共通科目", "2").copy(year="2025"))
        assertEquals("待核对，不计预览", SchoolCreditForecast.calculate(old, tables, 2026, setOf(1,2)).courses.single().state)
    }
    @Test fun retakeWithSameCodeInSecondSemesterKeepsBothResultsSeparate() {
        val d = "T1001 線形代数Ⅰ【機械】 教師 2.0単位"
        val saved = grades(g("線形代数Ⅰ", "2", false, 2), g("線形代数Ⅰ", "2", true, 4))
        val pages = listOf(table(1,d), table(2,d), table(3,d), table(4,d))
        val first = SchoolCreditForecast.calculate(saved,pages,2026,setOf(1,2))
        val second = SchoolCreditForecast.calculate(saved,pages,2026,setOf(3,4))
        assertEquals("不合格，不计新增", first.courses.single().state)
        assertEquals(2, first.courses.single().quarter)
        assertEquals("已修得，不重复增加", second.courses.single().state)
        assertEquals(4, second.courses.single().quarter)
        assertEquals(0, second.added.signum())
    }
    @Test fun firstAndSecondYearResultsNeverCrossMatch() {
        val d = "T1001 線形代数Ⅰ【機械】 教師 2.0単位"
        val saved = grades(g("線形代数Ⅰ", "2", false).copy(year="2025"), g("線形代数Ⅰ", "2"))
        val pages = listOf(table(2,d).copy(key="a/timetable-2025-Q2"), table(2,d))
        assertEquals("不合格，不计新增", SchoolCreditForecast.calculate(saved,pages,2025,setOf(1,2)).courses.single().state)
        assertEquals("已修得，不重复增加", SchoolCreditForecast.calculate(saved,pages,2026,setOf(1,2)).courses.single().state)
    }
    @Test fun sameCodeWithExplicitQuarterTagsIsNotMergedWithinSemester() {
        val saved = grades(g("共通科目", "2", false,1),g("共通科目", "2", true,2))
        val pages = listOf(table(1,"X1 共通科目【1Q】 教師 2.0単位"),table(2,"X1 共通科目【2Q】 教師 2.0単位"))
        val result = SchoolCreditForecast.calculate(saved,pages,2026,setOf(1,2))
        assertEquals(listOf("不合格，不计新增", "已修得，不重复增加"), result.courses.map { it.state })
    }
    @Test fun unknownFirstSemesterCourseDoesNotLeakIntoSecondSemester() {
        val result = SchoolCreditForecast.calculate(grades(),listOf(table(2,"X1 未知【学部】 教師 2.0単位")),2026,setOf(3,4))
        assertTrue(result.courses.isEmpty())
    }
    @Test fun oneResultCannotResolveTwoCourseCodes() {
        val result = SchoolCreditForecast.calculate(grades(g("共通科目", "2")),
            listOf(table(2, "X1 共通科目【学部】 教師 2.0単位", "X2 共通科目【学部】 教師 2.0単位")), 2026, setOf(1,2))
        assertTrue(result.courses.all { it.state == "成绩对应待核对，不计预览" })
        assertEquals(0, result.added.signum())
    }
    private fun g(name: String, credits: String, passed: Boolean = true, q: Int = 2) = PortalGrade(name, BigDecimal(credits), null, "", passed, "2026", "${q}クォーター", "")
    private fun grades(vararg g: PortalGrade) = PortalImport("a/grades", "", emptyList(), grades = g.toList())
    private fun table(q: Int, vararg descriptions: String, owner: String = "a") = PortalImport("$owner/timetable-2026-Q$q", "", emptyList(), lessons = descriptions.map { PortalLesson(1,1,it) })
    @Test fun semesterCourseCountsOnceAtEndDespiteManyMeetings() {
        val d = "T5001 線形代数Ⅰ【情報】 教師 2.0単位"
        val tables = listOf(table(1,d,d),table(2,d))
        assertEquals(0, SchoolCreditForecast.calculate(grades(),tables,2026,setOf(1)).added.signum())
        assertEquals(0, BigDecimal("2").compareTo(SchoolCreditForecast.calculate(grades(),tables,2026,setOf(1,2)).added))
    }
    @Test fun passedAndFailedResultsDoNotBecomeNewCredits() {
        val tables = listOf(table(2,"T5001 線形代数Ⅰ【情報】 教師 2.0単位","KB9185 学術日本語ⅡA【前期】 教師 1.0単位"))
        val result = SchoolCreditForecast.calculate(grades(g("線形代数Ⅰ","2"),g("学術日本語ⅡA","1",false)),tables,2026,setOf(2))
        assertEquals(0,result.added.signum())
        assertEquals(0,BigDecimal("2").compareTo(result.offered))
        assertEquals(0,BigDecimal("1").compareTo(result.failed))
        assertEquals(0,BigDecimal("2").compareTo(result.completed))
        assertEquals(0,BigDecimal("2").compareTo(result.total))
    }
    @Test fun futureCoursesAddToSavedTotalAndSeparateQuartersWithSameName() {
        val result = SchoolCreditForecast.calculate(grades(g("思想・文学","1",q=1)),
            listOf(table(1,"KB9304 思想・文学【1Q】 教師 1.0単位"),table(2,"KB9307 思想・文学【2Q】 教師 1.0単位")),2026,setOf(1,2))
        assertEquals(0,BigDecimal("1").compareTo(result.added))
        assertEquals(0,BigDecimal("2").compareTo(result.total))
    }
    @Test fun unavailableFutureAndOtherAccountsAreNeverInvented() {
        val result = SchoolCreditForecast.calculate(grades(),listOf(table(3,"T5004 化学【3Q】 教師 1.0単位", owner="b")),2026,setOf(3,4))
        assertTrue(result.courses.isEmpty())
        assertEquals(listOf(3,4),result.missing)
    }
    @Test fun currentSemesterUsesVerifiedDatesAndExcludesSummer() {
        assertNull(SchoolAcademicCalendar.semesterOn("2026-09-16"))
        assertEquals(setOf(1,2), SchoolAcademicCalendar.semesterOn("2026-06-01")!!.quarters)
        assertEquals(setOf(3,4), SchoolAcademicCalendar.semesterOn("2027-01-12")!!.quarters)
        assertNull(SchoolAcademicCalendar.semesterOn("2026-12-28"))
        assertNull(SchoolAcademicCalendar.semesterOn("2028-06-01"))
    }
    @Test fun projectedCategoriesDoNotChangeSavedEarnedAndKeepUnknownSeparate() {
        val grade = grades(g("思想・文学","1",q=1).copy(category="多文化理解"))
        val f = SchoolCreditForecast.calculate(grade, listOf(table(2,"KB9307 思想・文学【2Q】 教師 1.0単位", "KB9999 未知【2Q】 教師 2.0単位")),2026,setOf(2))
        val p = f.projectedCategories(grade)
        assertEquals(0, BigDecimal("1").compareTo(p.categories.single().earned))
        assertEquals(0, BigDecimal("2").compareTo(p.unassigned))
        assertEquals(0, BigDecimal("1").compareTo(f.earned))
        assertEquals(0, BigDecimal("3").compareTo(f.added))
    }
    @Test fun unknownDurationAndConflictingCreditsAreExcluded() {
        val result = SchoolCreditForecast.calculate(grades(),listOf(table(3,"X999 未知【情報】 教師 2.0単位","T5004 化学【3Q】 教師 1.0単位","T5004 化学【3Q】 教師 2.0単位")),2026,setOf(3))
        assertEquals(0,result.added.signum())
        assertTrue(result.courses.all { it.state.contains("待核对") })
    }
}

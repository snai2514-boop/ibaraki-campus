package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class EnrollmentRulesTest {
    @Test fun semesterLimitIsCombinedAcrossQuarters() {
        val r = EnrollmentRules.all.single { it.id == "multicultural" }
        assertTrue(EnrollmentRules.remaining(r, 1.toBigDecimal(), 1.toBigDecimal()).signum() < 0)
        assertTrue(r.scope.contains("Q3+Q4"))
        assertFalse(EnrollmentRules.all.any { it.title == "多文化理解" && it.limit != null })
    }
    @Test fun courseCountsCannotBeFractional() {
        val r = EnrollmentRules.all.single { it.id == "physical" }
        assertTrue(runCatching { EnrollmentRules.remaining(r, "0.5".toBigDecimal(), 0.toBigDecimal()) }.isFailure)
    }
    @Test fun allCapsAcceptBoundaryAndRejectOverflow() {
        EnrollmentRules.all.filter { it.limit != null }.forEach { r ->
            assertEquals(0, EnrollmentRules.remaining(r, r.limit!!, 0.toBigDecimal()).signum())
            assertTrue(EnrollmentRules.remaining(r, r.limit, 1.toBigDecimal()).signum() < 0)
        }
    }
    @Test fun englishNeedsBothPassedAtSameLevel() {
        fun g(s: String, pass: Boolean = true) = PortalGrade("Integrated English $s", 1.toBigDecimal(), if(pass) 70 else 30, "B", pass, "2026", "2クォーター", "english")
        assertTrue(EnrollmentRules.englishStatus(listOf(g("2A"),g("2B"))).startsWith("已保存成绩"))
        assertTrue(EnrollmentRules.englishStatus(listOf(g("2A"),g("2B",false))).startsWith("现有记录无法"))
        assertTrue(EnrollmentRules.englishStatus(listOf(g("1A"),g("2B"))).startsWith("现有记录无法"))
    }
}

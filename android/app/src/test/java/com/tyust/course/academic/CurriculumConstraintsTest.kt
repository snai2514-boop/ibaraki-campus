package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class CurriculumConstraintsTest {
    private fun rules(faculty: String, department: String, year: Int = 2026) =
        CurriculumConstraints.rules(CurriculumScope(faculty, department, year))

    @Test fun everySupportedFacultyAndCohortHasScopedRulesAndSources() {
        for (year in 2024..2026) for ((faculty, departments) in UniversityCurricula.departments) for (department in departments) {
            val rules = rules(faculty, department, year)
            assertTrue(rules.isNotEmpty())
            assertEquals(rules.size, rules.map { it.id }.distinct().size)
            assertTrue(rules.all { it.source.startsWith("https://") && it.page.isNotBlank() })
            assertEquals("1", rules.single { it.id == "multicultural" }.limit!!.toPlainString())
            assertTrue(rules.single { it.id == "annual_cap" }.scope.contains("学年度"))
        }
    }
    @Test fun mandatoryAndCombinationRulesDoNotLeakAcrossFacultiesOrYears() {
        assertTrue(rules("教育学部", "養護教諭養成課程").any { it.id == "constitution_required" })
        assertFalse(rules("工学部", "情報工学科").any { it.id == "constitution_required" })
        assertTrue(rules("工学部", "機械システム工学科").any { it.id == "environment_required" })
        assertFalse(rules("工学部", "情報工学科").any { it.id == "environment_required" })
        assertTrue(rules("地域未来共創学環", "地域未来共創学環").single { it.id == "culture_combination" }.detail.contains("不能"))
        assertTrue(rules("人文社会科学部", "人間文化学科").single { it.id == "foreign_language" }.detail.contains("一年级不能"))
        assertTrue(rules("工学部", "情報工学科", 2024).single { it.id == "physical" }.limit == null)
        assertEquals("1", rules("工学部", "情報工学科", 2025).single { it.id == "physical" }.limit!!.toPlainString())
        assertTrue(rules("工学部", "情報工学科", 2023).isEmpty())
    }

    @Test fun actualAwardsOnlyAndUnloadedGradesRemainUnknown() {
        val s = CurriculumScope("教育学部", "養護教諭養成課程", 2026)
        fun row(passed: Boolean, category: String = "基盤教育科目") = PortalGrade("日本国憲法", 2.toBigDecimal(), null, "A", passed, "2026", "前期", category)
        fun check(rows: List<PortalGrade>?) = CurriculumConstraintChecks.evaluate(s, rows).single { it.spec.id == "constitution" }
        assertEquals("尚未同步", check(null).status)
        assertEquals("已保存成绩尚不足", check(listOf(row(false))).status)
        assertEquals("已保存成绩尚不足", check(listOf(row(true, "卒業要件外"))).status)
        assertEquals("已满足学分数", check(listOf(row(true))).status)
        assertEquals("已保存成绩尚不足", check(listOf(row(true, "専門科目"))).status)
    }
}

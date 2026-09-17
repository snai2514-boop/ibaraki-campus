package com.tyust.course.academic

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class GraduationProgressTest {
    @Test fun optionalCreditsHaveNoRatioButStillCountTowardParentMinimum() {
        val requirements = listOf(CreditRequirement("group", "多文化理解", BigDecimal("3"), listOf(
            CreditRequirement("humanities", "ヒューマニティーズ", BigDecimal.ZERO),
            CreditRequirement("arts", "パフォーマンス＆アート", BigDecimal.ZERO)
        )))
        val group = GraduationProgressCalculator.calculate(requirements, listOf(
            AwardedCredit("demo", BigDecimal("2.0"), "humanities")
        )).categories.single()
        assertEquals("2/3", group.display)
        assertEquals("已修 2 学分", group.children[0].display)
        assertEquals("已修 0 学分", group.children[1].display)
        assertEquals(0, group.remaining.compareTo(BigDecimal.ONE))
        assertFalse(group.satisfied)
    }
    private fun n(value: String) = BigDecimal(value)
    private val requirements = listOf(CreditRequirement("total", "示例总要求", n("10"), listOf(
        CreditRequirement("culture", "示例文化类别", n("3")),
        CreditRequirement("major", "示例专业类别", n("7"))
    )))

    @Test fun excessInOneCategoryDoesNotSatisfyAnother() {
        val result = GraduationProgressCalculator.calculate(requirements, listOf(
            AwardedCredit("a", n("1"), "culture"), AwardedCredit("b", n("10"), "major")))
        val total = result.categories.single()
        assertEquals("11/10", total.display)
        assertFalse(total.satisfied)
        assertEquals("1/3", total.children.first().display)
        assertEquals(n("2"), total.children.first().remaining)
    }

    @Test fun unclassifiedCreditsDoNotSilentlyMeetRequirements() {
        val result = GraduationProgressCalculator.calculate(requirements, listOf(AwardedCredit("a", n("1.5"), null)))
        assertEquals(n("1.5"), result.unassigned)
        assertEquals("0/10", result.categories.single().display)
    }

    @Test(expected = IllegalArgumentException::class) fun duplicateCourseIsRejected() {
        GraduationProgressCalculator.calculate(requirements, listOf(
            AwardedCredit("a", n("2"), "culture"), AwardedCredit("a", n("2"), "major")))
    }

    @Test(expected = IllegalArgumentException::class) fun parentAssignmentIsRejected() {
        GraduationProgressCalculator.calculate(requirements, listOf(AwardedCredit("a", n("2"), "total")))
    }

    @Test fun allCategoriesMustMeetTheirThreshold() {
        val result = GraduationProgressCalculator.calculate(requirements, listOf(
            AwardedCredit("a", n("3"), "culture"), AwardedCredit("b", n("7"), "major")))
        assertTrue(result.categories.single().satisfied)
    }
}

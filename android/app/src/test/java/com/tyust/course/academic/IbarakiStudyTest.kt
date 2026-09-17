package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class IbarakiStudyTest {
    private fun record(id: String, score: Int?, kind: StudyGradeKind = StudyGradeKind.SCORED, category: String? = "culture_humanities", credits: String = "2") =
        StudyRecord(id, id, BigDecimal(credits), score, kind, category)
    @Test fun gpaUsesOfficialContinuousScaleAndIncludesFailures() {
        assertEquals(BigDecimal("2.25"), IbarakiStudyCalculator.gpa(listOf(record("a", 100), record("b", 59))))
        assertEquals(BigDecimal("0.50"), IbarakiStudyCalculator.gpa(listOf(record("a", 60))))
    }
    @Test fun gpaRoundsHalfUpAtTwoDecimalPlaces() {
        assertEquals(BigDecimal("3.13"), IbarakiStudyCalculator.gpa(listOf(record("a", 86, credits = "3"), record("b", 87, credits = "1"))))
    }
    @Test fun recognizedCreditsCountForProgressButNotGpa() {
        val records = listOf(record("a", null, StudyGradeKind.RECOGNIZED), record("b", 90))
        assertEquals(BigDecimal("3.50"), IbarakiStudyCalculator.gpa(records))
        assertEquals(BigDecimal("4"), IbarakiStudyCalculator.progress(records).categories.single().earned)
    }
    @Test fun pendingWithdrawnAndOutsideDoNotCountAsAwarded() {
        val records = listOf(record("a", null, StudyGradeKind.PENDING), record("b", null, StudyGradeKind.WITHDRAWN), record("c", null, StudyGradeKind.OUTSIDE))
        assertNull(IbarakiStudyCalculator.gpa(records))
        assertEquals(BigDecimal.ZERO, IbarakiStudyCalculator.progress(records).categories.single().earned)
    }
    @Test fun officialRequirementsContainCultureThreeAndTotal124() {
        val total = IbarakiStudyCalculator.progress(emptyList()).categories.single()
        val culture = total.children.first().children[1].children.first()
        assertEquals(BigDecimal("3"), culture.requirement.required)
        assertEquals(BigDecimal("124"), total.children.fold(BigDecimal.ZERO) { sum, child -> sum + child.requirement.required })
    }
    @Test fun oneCultureCreditProducesOneOfThreeAndTwoCreditSuggestion() {
        val suggestions = IbarakiStudyCalculator.suggestions(listOf(record("a", 90, credits = "1")))
        val culture = suggestions.single { it.requirement.id == "culture" }
        assertEquals("1/3", culture.display)
        assertEquals(BigDecimal("2"), culture.remaining)
        assertFalse(suggestions.any { it.requirement.id == "culture_humanities" })
    }
    @Test(expected = IllegalArgumentException::class) fun sameCourseCannotBeCountedTwice() {
        IbarakiStudyCalculator.gpa(listOf(record("a", 50), record("a", 90)))
    }
    @Test(expected = IllegalArgumentException::class) fun missingNumericScoreDoesNotBecomeZero() {
        IbarakiStudyCalculator.gpa(listOf(record("a", null)))
    }
}

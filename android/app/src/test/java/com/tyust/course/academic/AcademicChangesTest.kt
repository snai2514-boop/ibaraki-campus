package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class AcademicChangesTest {
    private fun snapshot(gpa: String, passed: Boolean = true, key: String = "a/grades") = PortalImport(key,"",listOf("学校通算 GPA：$gpa（学校值）"), grades=listOf(PortalGrade("课程",BigDecimal.ONE,60,"",passed,"2026","2クォーター","")))
    @Test fun firstImportAndDifferentStudentsNeverAlert() {
        assertTrue(AcademicChanges.changes(null,snapshot("2.7")).isEmpty())
        assertTrue(AcademicChanges.changes(snapshot("2.7"),snapshot("3.0",key="b/grades")).isEmpty())
    }
    @Test fun failedCourseBecomesEarnedAndGpaChanges() {
        val changes = AcademicChanges.changes(snapshot("2.7",false),snapshot("2.9"))
        assertEquals(2,changes.size)
        assertTrue(changes[0].contains("0 → 1"))
        assertTrue(changes[1].contains("2.7 → 2.9"))
    }
    @Test fun unchangedAndFormattingNeverAlertTwice() {
        assertTrue(AcademicChanges.changes(snapshot("2.70"),snapshot("2.7")).isEmpty())
    }
    @Test fun lowerGpaAlsoAlertsWithoutInventingCredits() {
        val changes=AcademicChanges.changes(snapshot("2.9"),snapshot("2.7"))
        assertEquals(1,changes.size)
        assertTrue(changes.single().startsWith("GPA"))
    }
    @Test fun missingGpaDoesNotBecomeZero() {
        assertTrue(AcademicChanges.changes(snapshot("2.7"),snapshot("—")).isEmpty())
    }
}

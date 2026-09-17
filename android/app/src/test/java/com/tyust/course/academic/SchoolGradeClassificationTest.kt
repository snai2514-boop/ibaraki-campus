package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolGradeClassificationTest {
    private fun grade(name: String, category: String, credits: String = "1", passed: Boolean = true, term: String = "2クォーター") =
        PortalGrade(name, credits.toBigDecimal(), if (passed) 80 else 13, if (passed) "A" else "D", passed, "2026", term, category)

    @Test fun missingEightCreditsAreAssignedWithoutMergingQuarterRows() {
        val grades = listOf(
            grade("思想・文学", "多文化理解", term = "1クォーター"),
            grade("思想・文学", "多文化理解"),
            grade("技術と社会", "自然と社会の広がり", term = "1クォーター"),
            grade("技術と社会", "自然と社会の広がり"),
            grade("ソフトウェア基礎", "学科共通専門基礎科目-必修", "2"),
            grade("コンピュータ基礎", "学科共通専門基礎科目-必修", "2"),
            grade("既存分類", "学部共通専門基礎教育科目", "17"),
            grade("学術日本語ⅡA", "多文化理解", passed = false))
        val records = SchoolSnapshotMigration.studyRecords(PortalImport("account/grades", "", emptyList(), grades))
        val progress = IbarakiStudyCalculator.progress(records)
        assertEquals(0, progress.categories.single().earned.compareTo("25".toBigDecimal()))
        assertEquals(0, progress.unassigned.signum())
        assertEquals(8, records.map { it.courseId }.distinct().size)
        assertEquals(listOf("culture_humanities", "culture_humanities", "nature", "nature", "department_core", "department_core"), records.take(6).map { it.categoryId })
    }

    @Test fun unknownOrConflictingCategoriesRemainVisibleAsUnassigned() {
        val grades = listOf(grade("未知の講義", "多文化理解"), grade("ソフトウェア基礎", "選択必修"))
        val progress = IbarakiStudyCalculator.progress(SchoolSnapshotMigration.studyRecords(PortalImport("a/grades", "", emptyList(), grades)))
        assertEquals(0, progress.unassigned.compareTo("2".toBigDecimal()))
        assertEquals(0, progress.categories.single().earned.signum())
    }

    @Test fun fullWidthSpacingIsNormalized() {
        assertEquals("department_core", SchoolGradeClassification.categoryId(grade("　ソフトウェア基礎　", "専門科目 / 学科共通専門基礎科目－必修")))
    }
}

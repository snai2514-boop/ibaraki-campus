package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class PortalImportParserTest {
    private fun grades(total: String = "2.0") = """
        修得単位数 | $total

        No. | 科目大区分 | 科目中区分 | 科目小区分 | 科目 | 単位数 | 修得年度 | 修得学期 | 評点 | 評語 | 合否
        1 | 基盤教育科目 | 基盤学修 | 大学入門ゼミ | テスト講義 | 2.0 | 2026 | 2クォーター | 80 | A | 合
        2 | 基盤教育科目 | 主体学修 | 多文化理解 | 別の講義 | 1.0 | 2026 | 2クォーター | 13 | D | 否

        年度・学期 | 学期GPA | 年間GPA | 通算GPA
        2026年度前期 | 2.00 | - | 2.00
    """.trimIndent()
    private fun timetable() = buildString {
        append("年度・学期 | 2026年度 2クォーター | 件数 | 1件\n\n")
        append(" | 月曜日 | 火曜日 | 水曜日 | 木曜日 | 金曜日 | 土曜日\n\n")
        for (p in 1..6) {
            append("${p}限\n\n")
            for (d in 1..6) append(if (p == 2 && d == 3) "T5005 テスト講義 教員 2.0単位\n\n" else "未登録\n\n")
        }
        append("集中講義など\n登録されていません")
    }
    @Test fun failedCreditsAreExcludedAndOfficialGpaPreserved() {
        val result = PortalImportParser.parse(grades())
        assertTrue(result.title.contains("2.0 学分"))
        assertTrue(result.cards[0].contains("2.00"))
        assertTrue(result.cards.last().contains("未通过"))
    }
    @Test fun incompleteGradesFailClosed() {
        assertTrue(runCatching { PortalImportParser.parse(grades("3.0")) }.isFailure)
    }
    @Test fun timetablePreservesQuarterAndCoordinates() {
        val result = PortalImportParser.parse(timetable())
        assertEquals("timetable-2026-Q2", result.key)
        assertTrue(result.cards.last().startsWith("星期三 · 第 2 限"))
    }
    @Test fun exactDomCourseNameIsKeptSeparatelyAndUnrelatedMetadataIsIgnored() {
        val lesson=PortalImportParser.parse(timetable(),courseNames=mapOf("T5005" to "テスト講義")).lessons.single()
        assertEquals("テスト講義",lesson.courseName)
        assertTrue(lesson.description.contains("教員"))
        assertTrue(PortalImportParser.parse(timetable(),courseNames=mapOf("T5005" to "別の講義")).lessons.single().courseName.isEmpty())
    }
    @Test fun officialClassSuffixesArePreservedAcrossDepartments() {
        for (code in listOf("T1018-A", "T5008-e", "T1066-H32B", "LA0001", "AN1003")) {
            assertTrue(PortalImportParser.parse(timetable().replace("T5005",code)).lessons.single().description.startsWith("$code "))
        }
    }
    @Test fun shiftedColumnsAndCountMismatchAreRejected() {
        assertTrue(runCatching { PortalImportParser.parse(timetable().replaceFirst("未登録\n\n", "")) }.isFailure)
        assertTrue(runCatching { PortalImportParser.parse(timetable().replace("1件", "2件")) }.isFailure)
    }
    @Test fun registrationActionRowsDoNotShiftTheTimetable() {
        val open = timetable().replace("2.0単位\n\n", "2.0単位\n\n追加登録\n\n")
            .replace("集中講義など\n", "集中講義など | 集中講義を登録\n")
        assertEquals(PortalImportParser.parse(timetable()).lessons, PortalImportParser.parse(open).lessons)
        assertTrue(runCatching { PortalImportParser.parse(open.replaceFirst("未登録\n\n", "")) }.isFailure)
        assertTrue(runCatching { PortalImportParser.parse(open.replace("1件", "2件")) }.isFailure)
    }
    @Test fun loginOrUnknownPageCannotClearRecords() {
        assertTrue(runCatching { PortalImportParser.parse("ログイン") }.isFailure)
    }
    @Test fun emptyTimetableIsOnlyAcceptedForExplicitSyncInspection() {
        val empty = timetable().replace("1件", "0件").replace("T5005 テスト講義 教員 2.0単位", "未登録")
        assertTrue(runCatching { PortalImportParser.parse(empty) }.isFailure)
        assertTrue(PortalImportParser.parse(empty, allowEmptyTimetable = true).lessons.isEmpty())
        assertTrue(runCatching { PortalImportParser.parse(empty.replaceFirst("未登録\n\n", ""), allowEmptyTimetable = true) }.isFailure)
    }
    @Test fun oldSnapshotsRecoverStructuredGradesAndTimetable() {
        val grade = PortalImportParser.parse(grades())
        assertEquals(grade.grades, SchoolSnapshotMigration.expand(grade.copy(grades = emptyList())).grades)
        val table = PortalImportParser.parse(timetable())
        assertEquals(table.lessons, SchoolSnapshotMigration.expand(table.copy(lessons = emptyList())).lessons)
    }
    @Test fun graduationCountsOnlyPassedAndDoesNotGuessBroadCategories() {
        val grade = PortalImportParser.parse(grades())
        val records = SchoolSnapshotMigration.studyRecords(grade)
        assertEquals("seminar", records.first().categoryId)
        assertNull(records.last().categoryId)
        val progress = IbarakiStudyCalculator.progress(records)
        assertEquals(0, progress.categories.single().earned.compareTo("2.0".toBigDecimal()))
    }
}

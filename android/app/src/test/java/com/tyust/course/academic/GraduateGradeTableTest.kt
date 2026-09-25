package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class GraduateGradeTableTest {
    private val text="""
        修得単位数 | 10

        No. | 科目大区分 | 科目中区分 | 科目名 | 単位数 | 修得年度 | 修得学期 | 評語 | 合否
        1 | 専攻科目 | コース共通専攻科目 | 専攻研究 | 10 | 2026 | 後期 | 認 | 合

    """.trimIndent()
    @Test fun graduateMissingMinorCategoryAndNumericScoreCanBeRead() {
        val r=PortalImportParser.parse(text)
        assertEquals("専攻研究",r.grades.single().name)
        assertNull(r.grades.single().score)
        assertTrue(r.cards.first().contains("未提供"))
        assertEquals("専攻科目 / コース共通専攻科目",r.grades.single().category)
    }
    @Test fun reorderedFieldsUseHeadersNotOffsets() {
        val r=PortalImportParser.parse("修得単位数 | 0.5\n\n科目区分 | 科目 | 合否 | 評語 | 修得学期 | 修得年度 | 単位\n必修科目 | 合同セミナー | 合格 | S | 前期 | 2026 | 0.5")
        assertEquals("0.5",r.grades.single().credits.toPlainString());assertTrue(r.grades.single().passed)
    }
    @Test fun malformedMissingOrDuplicateDataNeverOverwrites() {
        listOf(text.replace(" | 10 | 2026", " | 2026"),text.replace("修得単位数 | 10","修得単位数 | 12"),text.replace("合否","未知列"),
            text.replace("科目中区分","科目大区分"),text.replace(" | 合"," | 未確定"),text.replace("専攻研究 | 10","専攻研究 | -1")).forEach {
            assertTrue(it,runCatching { PortalImportParser.parse(it) }.isFailure)
        }
    }
    @Test fun duplicateRowsAndConflictingScoresAreRejected() {
        val duplicate=text+"\n"+text.lines().last { it.startsWith("1 |") }
        assertTrue(runCatching { PortalImportParser.parse(duplicate) }.isFailure)
        assertTrue(runCatching { PortalImportParser.parse(text.replace("評語 |", "評点 | 評語 |").replace("認 | 合", "45 | D | 合")) }.isFailure)
    }
}

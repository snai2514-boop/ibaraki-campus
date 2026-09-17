package com.tyust.course.academic
import org.junit.Assert.*
import org.junit.Test
class SyllabusSummaryTest {
 @Test fun preservesExplicitNoExamInsteadOfInferringFromKeyword() {
  assertEquals("期末試験は実施しません", SyllabusSummary.exam("期末試験は実施しません。小テスト４回(各15%)", ""))
 }
 @Test fun absentExamIsUnknown() { assertTrue(SyllabusSummary.exam("レポート100%", "第1回 授業").contains("未明确")) }
 @Test fun keepsConditionalExamWording() { assertTrue(SyllabusSummary.exam("期末試験は必要に応じて実施する", "").contains("必要に応じて")) }
}

package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolStudentProfileTest {
    @Test fun readsStudentRecordFieldsWithoutInferringYearFromNumber() {
        val p = SchoolStudentProfileParser.parse("学生番号 | 26T0000X | 学生氏名 | TEST STUDENT\n所属 | 工学部情報工学科 | 学年 | 2年")!!
        assertEquals("TEST STUDENT", p.name); assertEquals("工学部", p.faculty)
        assertEquals("情報工学科", p.department); assertEquals("2年", p.year)
    }
    @Test fun refusesConflictingIdentitiesAndUnrelatedPages() {
        assertNull(SchoolStudentProfileParser.parse("学生番号 | 26T0000X | 学生氏名 | TEST\n学生番号 | 26T9999X"))
        assertNull(SchoolStudentProfileParser.parse("氏名 | TEST | 学生番号 | 26T0000X"))
        assertNull(SchoolStudentProfileParser.parse("学生氏名 | TEST"))
    }
    @Test fun missingAffiliationIsNotInvented() {
        val p = SchoolStudentProfileParser.parse("学生番号 | 26T0000X | 学生氏名 | TEST")!!
        assertEquals("", p.year); assertEquals("", p.department)
    }
}

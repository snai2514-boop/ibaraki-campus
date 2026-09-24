package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolRegistrationReadingTest {
    private fun table()=buildString {
        append("学生番号 | A123\n年度・学期 | 2026年度 3クォーター | 件数 | 1件\n\n")
        append(" | 月曜日 | 火曜日 | 水曜日 | 木曜日 | 金曜日 | 土曜日\n\n")
        for(p in 1..6) {
            append("${p}限\n\n")
            for(d in 1..6) append(if(p==1 && d==5) "KB9999 テスト講義 教員 2.0単位\n\n" else "未登録\n\n")
        }
        append("集中講義など\n登録されていません")
    }
    @Test fun validReadOnlyTimetableIsStatusWithoutInventedEligibility() {
        val result=SchoolRegistrationReading.readOnly(table(),"A123")!!
        assertEquals("2026-Q3",result.scope)
        assertEquals("金 1",result.rows.single().schedule)
        assertFalse(result.rows.single().available)
        assertEquals("已登记",result.rows.single().status)
    }
    @Test fun wrongAccountMalformedAndLoginPagesAreNotSuccessfulReads() {
        assertNull(SchoolRegistrationReading.readOnly(table(),"B456"))
        assertNull(SchoolRegistrationReading.readOnly(table().replace("1件","2件"),"A123"))
        assertNull(SchoolRegistrationReading.readOnly("ログイン","A123"))
    }
    @Test fun openGridExplainsDirectSchoolRegistrationWithoutInventingBatchEligibility() {
        val open=table().replace("件数 |", "登録期限 | 2026年10月1日 23時59分 | 件数 |")
            .replace("2.0単位\n\n", "2.0単位\n\n追加登録\n\n")
            .replace("集中講義など\n", "集中講義など | 集中講義を登録\n")
        val result=SchoolRegistrationReading.readOnly(open,"A123")!!
        assertTrue(result.message.contains("可登录课程"))
        assertFalse(result.rows.single().available)
    }
}

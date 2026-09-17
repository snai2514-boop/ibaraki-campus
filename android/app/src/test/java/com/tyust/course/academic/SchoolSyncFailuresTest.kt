package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolSyncFailuresTest {
    @Test fun unconfirmedFailuresAreNotBlamedOnTheNetwork() {
        assertEquals(SchoolSyncFailureKind.UNKNOWN, SchoolSyncFailures.classify("学校可能需要重新认证或网络不可用"))
        assertEquals(SchoolSyncFailureKind.TIMEOUT, SchoolSyncFailures.classify("Q3 未读取成功：学校响应超时"))
        assertEquals(SchoolSyncFailureKind.DATA, SchoolSyncFailures.classify("明细学分与学校总数不一致"))
        assertEquals(SchoolSyncFailureKind.STORAGE, SchoolSyncFailures.classify("保存失败，原记录已保留"))
        assertTrue(SchoolSyncFailureKind.PROGRAM.feedback)
        assertFalse(SchoolSyncFailureKind.NETWORK.feedback)
    }
    @Test fun failuresKeepModuleIdentityAcrossScreenChangesUntilDismissed() {
        SchoolSyncFailures.pending.value = emptyList()
        SchoolSyncFailures.report(SchoolSyncFailures.part(0), "成绩明细不完整")
        SchoolSyncFailures.report(SchoolSyncFailures.part(3), "学校响应超时")
        SchoolSyncFailures.report(SchoolSyncFailures.part(0), "成绩明细不完整")
        assertEquals(listOf("成绩与学分", "Q3 课表"), SchoolSyncFailures.pending.value.map { it.part })
        SchoolSyncFailures.dismiss(SchoolSyncFailures.pending.value.first())
        assertEquals("Q3 课表", SchoolSyncFailures.pending.value.single().part)
        SchoolSyncFailures.pending.value = emptyList()
    }
}

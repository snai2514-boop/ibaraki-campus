package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class RegistrationBatchTest {
    private val courses=(1..3).map {n -> RegistrationCourse("2026-Q3/03/C$n","C$n","课程$n",
        if(n<3) "月1" else "火2","2","",true,slotKey=if(n<3) "1-1" else "2-2",signature="signature$n")}
    private val snapshot=RegistrationSnapshot("A123","2026-Q3","","native:2026-Q3",courses,100)
    private fun batch()=RegistrationBatch.approve(snapshot,courses.map {it.id}.toSet())
    private fun RegistrationBatch.send(id: String)=beginCurrent(id).dispatched(id)

    @Test fun allCoursesSendBeforeOneFinalVerificationIncludingSameSlot() {
        var state=batch()
        val dispatched=mutableListOf<String>()
        while(state.current!=null) {
            val next=state.current!!
            dispatched+=next.id
            state=state.send(next.id)
            assertTrue(state.confirmedIds.isEmpty())
            assertFalse(state.verified)
        }
        assertEquals(courses.map {it.id},dispatched)
        assertTrue(state.remaining.isEmpty())
        state=state.verify(courses.map {it.code}.toSet())
        assertEquals(3,state.confirmedIds.size)
        assertTrue(state.verified)
        assertNull(state.current)
    }

    @Test fun aRejectedCourseDoesNotPreventSendingTheRestAndIsReportedAtTheEnd() {
        val state=courses.fold(batch()) {b,c -> b.send(c.id)}.verify(setOf("C1","C3"))
        assertEquals(3,state.sentIds.size)
        assertEquals(setOf(courses[0].id,courses[2].id),state.confirmedIds)
        assertTrue(state.report("统一结果").contains("未登记（学校课表未显示）：课程2"))
        assertFalse(state.report("统一结果").contains("未发送"))
    }

    @Test fun ambiguousDispatchBlocksFurtherSendsButCanBeResolvedByFinalQuery() {
        val state=batch().send(courses[0].id).beginCurrent(courses[1].id)
        assertNull(state.current)
        assertThrows(IllegalArgumentException::class.java) {state.beginCurrent(courses[2].id)}
        val final=state.stop().verify(setOf("C1","C2"))
        assertEquals(setOf(courses[0].id,courses[1].id),final.confirmedIds)
        assertTrue(final.report("连接中断").contains("未发送：课程3"))
    }

    @Test fun restartOnlyQueriesDurableAttemptedCoursesAndNeverResumesSubmission() {
        val before=batch().send(courses[0].id).beginCurrent(courses[1].id)
        val recovered=RegistrationBatch.recover(before.courses,before.attemptedIds,before.sentIds)
        assertNull(recovered.current)
        assertThrows(IllegalArgumentException::class.java) {recovered.dispatched(courses[1].id)}
        val final=recovered.verify(setOf("C1","C3","unrelated"))
        assertEquals(setOf(courses[0].id),final.confirmedIds)
        assertEquals(listOf(courses[2]),final.remaining)
    }

    @Test fun unrelatedOrDuplicateDispatchCannotAdvanceQueue() {
        assertThrows(IllegalArgumentException::class.java) {batch().beginCurrent(courses[2].id)}
        assertThrows(IllegalArgumentException::class.java) {batch().dispatched(courses[0].id)}
        val state=batch().send(courses[0].id)
        assertThrows(IllegalArgumentException::class.java) {state.beginCurrent(courses[0].id)}
        assertThrows(IllegalArgumentException::class.java) {state.dispatched(courses[0].id)}
        assertEquals(courses[1],state.current)
    }

    @Test fun queryFailurePreservesUnconfirmedStatusAndRequeryCanResolveIt() {
        val state=batch().send(courses[0].id).stop()
        assertTrue(state.report("查询超时").contains("等待统一查询：课程1"))
        assertFalse(state.report("查询超时").contains("已登记"))
        val result=state.verify(emptySet()).verify(setOf("C1"))
        assertEquals(setOf(courses[0].id),result.confirmedIds)
    }

    @Test fun finalRefusalShowsCompletedOutcomeSchoolReasonAndQuota() {
        val sent=batch().send(courses[0].id).recordFeedback(courses[0].id,"履修登録可能単位数を超えています")
        val recovered=RegistrationBatch.recover(sent.courses,sent.attemptedIds,sent.sentIds,sent.feedback)
        val report=recovered.verify(emptySet()).report("学校查询结果","1.0")
        assertTrue(report.contains("查询已完成：0 门已登记，1 门未登记"))
        assertTrue(report.contains("学校当前还可登记 1.0 学分"))
        assertTrue(report.contains("学校反馈：履修登録可能単位数を超えています"))
        assertFalse(report.contains("等待统一查询"))
        assertFalse(report.contains("已发送"))
        assertFalse(recovered.verify(setOf("C1")).report("完成").contains("学校反馈"))
        assertFalse(recovered.verify(emptySet()).report("完成").contains("还可登记"))
    }

    @Test fun malformedRecoveryCannotInventAttempts() {
        assertThrows(IllegalArgumentException::class.java) {RegistrationBatch.recover(courses,setOf("unknown"),emptySet())}
        assertThrows(IllegalArgumentException::class.java) {RegistrationBatch.recover(courses,emptySet(),setOf(courses[0].id))}
    }

    @Test fun changedUnavailableOrUnknownSelectionCannotStartBatch() {
        assertThrows(IllegalArgumentException::class.java) {RegistrationBatch.approve(snapshot,setOf("unknown"))}
        assertThrows(IllegalArgumentException::class.java) {RegistrationBatch.approve(snapshot,emptySet())}
        val changed=snapshot.copy(rows=listOf(courses[0].copy(available=false)))
        assertThrows(IllegalArgumentException::class.java) {RegistrationBatch.approve(changed,setOf(courses[0].id))}
        val ambiguous=snapshot.copy(rows=listOf(courses[0],courses[0]))
        assertThrows(IllegalArgumentException::class.java) {RegistrationBatch.approve(ambiguous,setOf(courses[0].id))}
    }
}

package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class RegistrationPolicyTest {
    private val course=RegistrationCourse("2026/Q3/ABC","ABC","课程","金 1","2","",true)
    private fun snapshot(student: String="A", scope: String="2026/Q3", rows: List<RegistrationCourse> = listOf(course)) =
        RegistrationSnapshot(student,scope,"","",rows,100)
    @Test fun firstOpenAndNewCourseAlertButRepeatedReadDoesNot() {
        assertEquals(listOf(course),RegistrationPolicy.newAvailable(null,snapshot()))
        assertTrue(RegistrationPolicy.newAvailable(snapshot(),snapshot()).isEmpty())
        assertEquals(listOf(course),RegistrationPolicy.newAvailable(snapshot(rows=listOf(course.copy(available=false))),snapshot()))
        assertTrue(RegistrationPolicy.newAvailable(snapshot(),snapshot(rows=emptyList())).isEmpty())
    }
    @Test fun accountsAndTermsNeverShareBaseline() {
        assertEquals(1,RegistrationPolicy.newAvailable(snapshot(student="B"),snapshot()).size)
        assertEquals(1,RegistrationPolicy.newAvailable(snapshot(scope="2025/Q3"),snapshot()).size)
    }
    @Test fun noInventedDayForIntensiveAndAllActualDaysRemain() {
        assertEquals(listOf(1,3,5),RegistrationPolicy.days("月・水・金 1時限"))
        assertTrue(RegistrationPolicy.days("集中／未定").isEmpty())
        assertEquals(listOf(7),RegistrationPolicy.days("日 2"))
    }
    @Test fun staleAndFutureSnapshotsCannotSubmit() {
        assertTrue(RegistrationPolicy.fresh(100,101))
        assertFalse(RegistrationPolicy.fresh(100,300100))
        assertFalse(RegistrationPolicy.fresh(100,99))
        assertFalse(RegistrationPolicy.fresh(0,100))
    }
    @Test fun calendarCellsKeepDayPeriodPairsAndNormalizeFullWidth() {
        assertEquals(setOf(RegistrationSlot(1,1),RegistrationSlot(3,2)),RegistrationPolicy.slots("月1 水2"))
        assertEquals(setOf(RegistrationSlot(5,1)),RegistrationPolicy.slots("金曜日 １時限"))
        assertEquals(setOf(RegistrationSlot(1,1),RegistrationSlot(1,2)),RegistrationPolicy.slots("月 1-2"))
        assertEquals(setOf(RegistrationSlot(1,1),RegistrationSlot(3,1)),RegistrationPolicy.slots("月・水 1"))
        assertTrue(RegistrationPolicy.slots("集中／未定").isEmpty())
        assertTrue(RegistrationPolicy.slots("月10").isEmpty())
        assertEquals(listOf(5),RegistrationPolicy.days("金曜日"))
    }
    @Test fun dialogSelectionSurvivesIdenticalBackgroundReadButRejectsChangedRows() {
        assertEquals(setOf(course.id),RegistrationPolicy.reconcile(setOf(course.id),snapshot(),snapshot()))
        assertTrue(RegistrationPolicy.reconcile(setOf(course.id),snapshot(),snapshot(rows=listOf(course.copy(schedule="火 2")))).isEmpty())
        assertTrue(RegistrationPolicy.reconcile(setOf(course.id),snapshot(),snapshot(student="B")).isEmpty())
        assertTrue(RegistrationPolicy.reconcile(setOf(course.id),snapshot(),snapshot(rows=emptyList())).isEmpty())
    }
}

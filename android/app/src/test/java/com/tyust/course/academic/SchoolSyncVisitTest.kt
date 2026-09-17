package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolSyncVisitTest {
    @Test fun openingSyncsOnceAndPageChangesCannotRestartIt() {
        val visits = SchoolSyncVisit()
        visits.opened()
        assertTrue(visits.consume())
        repeat(10) { assertFalse(visits.consume()) }
        visits.opened()
        assertTrue(visits.consume())
        assertFalse(visits.consume())
    }
}

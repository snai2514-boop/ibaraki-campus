package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolNoticeNotifierTest {
    private val row = SchoolNotice("1", "Notice", "2026/09/16 10:00", "2026/09/16–2026/10/01")
    @Test fun initialImportDoesNotAlertForHistory() {
        assertTrue(SchoolNoticeNotifier.changed(SchoolNotices(), listOf(row)).isEmpty())
    }
    @Test fun repeatedSyncAndBodyLoadDoNotRepeatAlert() {
        val old = SchoolNotices(listOf(row), 1, true)
        assertTrue(SchoolNoticeNotifier.changed(old, listOf(row.copy(body = "Full text"))).isEmpty())
    }
    @Test fun newAndRevisedNoticesAlertButRemovedNoticesDoNot() {
        val old = SchoolNotices(listOf(row), 1, true)
        val revised = row.copy(published = "2026/09/17 10:00")
        val added = row.copy(id = "2")
        assertEquals(listOf(revised, added), SchoolNoticeNotifier.changed(old, listOf(revised, added)))
        assertTrue(SchoolNoticeNotifier.changed(old, emptyList()).isEmpty())
    }
}

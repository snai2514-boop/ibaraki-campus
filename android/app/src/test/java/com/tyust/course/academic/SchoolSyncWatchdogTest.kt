package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolSyncWatchdogTest {
    @Test fun silentJavaScriptOrNavigationCannotLeaveSyncRunningForever() {
        assertFalse(SchoolSyncWatchdog.expired(60_999, 1_000, true))
        assertTrue(SchoolSyncWatchdog.expired(61_000, 1_000, true))
        assertTrue(SchoolSyncWatchdog.expired(600_000, 1_000, true))
    }
    @Test fun eachStageGetsItsOwnDeadlineWithoutTimingOutInteractiveLogin() {
        assertFalse(SchoolSyncWatchdog.expired(600_000, 590_000, true))
        assertFalse(SchoolSyncWatchdog.expired(600_000, 1_000, false))
        assertFalse(SchoolSyncWatchdog.expired(600_000, 0, true))
    }
}

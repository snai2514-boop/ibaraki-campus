package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class DailySchoolSyncPolicyTest {
    private fun t(s: String) = Instant.parse(s).toEpochMilli()
    @Test fun sameTokyoDayDoesNotRepeatAfterRestart() {
        assertFalse(DailySchoolSyncPolicy.due(t("2026-09-16T14:59:00Z"), t("2026-09-15T15:01:00Z"), 0))
    }
    @Test fun nextTokyoDayUpdates() {
        assertTrue(DailySchoolSyncPolicy.due(t("2026-09-16T15:01:00Z"), t("2026-09-16T01:00:00Z"), t("2026-09-16T01:00:00Z")))
    }
    @Test fun failuresAreThrottledButCanRetry() {
        assertFalse(DailySchoolSyncPolicy.due(7_000_000, 0, 6_000_000))
        assertTrue(DailySchoolSyncPolicy.due(10_000_000, 0, 6_000_000))
        assertTrue(DailySchoolSyncPolicy.due(7_000_000, 0, 0))
    }
}

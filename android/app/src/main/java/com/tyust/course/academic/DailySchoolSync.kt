package com.tyust.course.academic

import android.content.Context

object DailySchoolSyncPolicy {
    private fun tokyoDay(time: Long) = Math.floorDiv(time + 9 * 3_600_000L, 86_400_000L)
    fun due(now: Long, lastSuccess: Long, lastAttempt: Long): Boolean {
        if (lastSuccess > 0 && tokyoDay(lastSuccess) == tokyoDay(now)) return false
        // Retry failures at most hourly, including across repeated activity resumes.
        return lastAttempt <= 0 || now < lastAttempt || now - lastAttempt >= 3_600_000
    }
}

class DailySchoolSync(context: Context) {
    private val prefs = context.getSharedPreferences("daily-school-sync", Context.MODE_PRIVATE)
    fun due() = DailySchoolSyncPolicy.due(System.currentTimeMillis(), prefs.getLong("success", 0), prefs.getLong("attempt", 0))
    fun attempted() { prefs.edit().putLong("attempt", System.currentTimeMillis()).apply() }
    fun succeeded() { prefs.edit().putLong("success", System.currentTimeMillis()).apply() }
}

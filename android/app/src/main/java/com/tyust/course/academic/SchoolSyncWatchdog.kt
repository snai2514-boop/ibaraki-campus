package com.tyust.course.academic

/** An authenticated stage must end even if WebView never calls back. Login itself has no deadline. */
object SchoolSyncWatchdog {
    fun expired(now: Long, stageStarted: Long, authenticated: Boolean): Boolean =
        authenticated && stageStarted > 0 && now - stageStarted >= 60_000L
}

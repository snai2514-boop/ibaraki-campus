package com.tyust.course.academic

/** One sync per foreground visit; page changes do not create another visit. */
class SchoolSyncVisit {
    private var visit = 0L
    private var consumed = -1L
    fun opened() { visit++ }
    fun consume(): Boolean {
        if (consumed == visit) return false
        consumed = visit
        return true
    }
}

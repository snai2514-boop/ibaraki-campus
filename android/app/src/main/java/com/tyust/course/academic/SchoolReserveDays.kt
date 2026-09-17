package com.tyust.course.academic

/** Official 2026 academic-year calendar, lessonplan.pdf pp2–4. Notices, not scheduled lessons. */
object SchoolReserveDays {
    const val source = "https://www.hum.ibaraki.ac.jp/pdf/lessonplan.pdf"
    fun on(date: String): String? = when (date) {
        "2026-05-30", "2026-05-31", "2026-06-06", "2026-06-07", "2026-06-09" -> "季度课程预备日"
        "2026-07-28" -> "学期课程预备日"
        "2026-07-23", "2026-07-24", "2026-07-29", "2026-07-30", "2026-07-31" -> "课程预备日"
        "2026-11-18", "2026-11-19", "2026-11-20", "2026-11-24", "2026-11-28" -> "季度课程预备日"
        "2027-01-20", "2027-01-21", "2027-01-26", "2027-01-29" -> "学期课程预备日"
        "2027-01-27", "2027-01-28", "2027-02-01" -> "季度课程预备日"
        "2027-01-30" -> "课程预备日"
        else -> null
    }
}

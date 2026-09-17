package com.tyust.course.academic

/** University-wide published academic-calendar breaks. Never infer from an empty timetable. */
data class SchoolBreak(val name: String, val start: String, val end: String)
object SchoolHolidays {
    // 2025 university academic calendar; 2026 university calendar reproduced on p2 of lessonplan.pdf.
    val breaks = listOf(
        SchoolBreak("暑假", "2025-08-12", "2025-09-20"),
        SchoolBreak("寒假", "2025-12-27", "2026-01-05"),
        SchoolBreak("春假", "2026-02-24", "2026-03-31"),
        SchoolBreak("暑假", "2026-08-12", "2026-09-20"),
        SchoolBreak("寒假", "2026-12-27", "2027-01-05"),
        SchoolBreak("春假", "2027-02-24", "2027-03-31")
    )
    fun phoneDate(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
    fun classroomNotice(date: String): String? = on(date)?.let {
        "假期期间暂无法查看教室信息"
    }
    fun classroomReason(date: String): String? = on(date)?.let {
        "手机日期 $date 处于学校规定的${it.name}（${it.start} 至 ${it.end}）。教室具体信息可能尚未开放，本次暂不读取；已保存的教室记录保留，其他模块继续同步。"
    }
    fun on(date: String): SchoolBreak? = breaks.firstOrNull { date in it.start..it.end }
}

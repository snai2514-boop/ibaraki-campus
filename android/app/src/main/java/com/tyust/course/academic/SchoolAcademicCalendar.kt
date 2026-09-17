package com.tyust.course.academic

/** 2026 official calendar printed pp1–2 and engineering offerings pp3–21.
 * Explicit campus dates include replacement weekdays and exclude reserve days.
 * Semester courses are sliced without overlap between quarters, including S8 on quarter reserve days.
 */
object SchoolAcademicCalendar {
    fun creditQuarter(year: Int, code: String, description: String): Int? {
        Regex("【([1-4])Q】").find(description)?.let { return it.groupValues[1].toInt() }
        if (description.contains("【前期】") || description.contains("【前学期】")) return 2
        if (description.contains("【後期】") || description.contains("【後学期】") || description.contains("【通年】")) return 4
        if (year != 2026) return null
        return when (SchoolCourseOfferings2026.find(code)?.term) {
            "前期" -> 2
            "後期", "通年" -> 4
            "1Q" -> 1
            "2Q" -> 2
            "3Q" -> 3
            "4Q" -> 4
            else -> null
        }
    }
    private val dates = mapOf(
        1 to listOf(
            "04-13 04-20 04-27 05-11 05-18 05-25 06-01",
            "04-14 04-21 04-28 05-12 05-19 05-26 06-02",
            "04-15 04-22 05-01 05-13 05-20 05-27 06-03",
            "04-16 04-23 04-30 05-07 05-14 05-21 05-28",
            "04-10 04-17 04-24 05-08 05-15 05-22 05-29"),
        2 to listOf(
            "06-08 06-15 06-22 06-29 07-06 07-13 07-27",
            "06-16 06-23 06-30 07-07 07-14 07-21 07-28",
            "06-10 06-17 06-24 07-01 07-08 07-15 07-22",
            "06-04 06-11 06-18 06-25 07-02 07-09 07-16",
            "06-05 06-12 06-19 06-26 07-03 07-10 07-17"),
        3 to listOf(
            "09-28 10-05 10-19 10-26 11-02 11-09 11-16",
            "09-29 10-06 10-13 10-20 10-27 11-10 11-17",
            "09-30 10-07 10-14 10-21 10-28 11-04 11-11",
            "10-01 10-08 10-15 10-22 10-29 11-05 11-12",
            "09-25 10-02 10-09 10-16 10-23 10-30 11-06"),
        4 to listOf(
            "11-30 12-07 12-14 12-21 01-06 01-18 01-25",
            "12-01 12-08 12-15 12-22 01-12 01-19 01-26",
            "11-25 12-02 12-09 12-16 12-23 01-13 01-20",
            "11-26 12-03 12-10 12-17 12-24 01-07 01-21",
            "11-27 12-04 12-11 12-18 12-25 01-08 01-29"))

    data class Semester(val year: Int, val quarters: Set<Int>, val label: String)
    /** Use the verified teaching dates, never infer an unpublished year's calendar. */
    fun semesterOn(date: String): Semester? {
        if (SchoolHolidays.on(date) != null) return null
        return listOf(setOf(1, 2), setOf(3, 4)).firstNotNullOfOrNull { quarters ->
            val actual = quarters.flatMap { q -> dates.getValue(q).flatMap { it.split(' ') }.map {
                "${if (q == 4 && it.substringBefore('-').toInt() < 4) 2027 else 2026}-$it"
            } }
            if (date in actual.min()..actual.max()) Semester(2026, quarters, if (1 in quarters) "前期 Q1+Q2" else "后期 Q3+Q4") else null
        }
    }

    data class Issue(val lesson: PortalLesson, val reason: String)
    data class Resolution(val events: List<SchoolCalendarEvent>, val issues: List<Issue>)

    fun resolve(item: PortalImport, profile: SchoolStudentProfile? = null): Resolution {
        val issues = mutableListOf<Issue>()
        val events = item.lessons.flatMap { lesson ->
            try { lessonEvents(item, lesson, profile) }
            catch (e: IllegalArgumentException) { issues += Issue(lesson, e.message.orEmpty()); emptyList() }
            catch (e: IllegalStateException) { issues += Issue(lesson, e.message.orEmpty()); emptyList() }
        }.sortedWith(compareBy({ it.date }, { it.lesson.period }))
        return Resolution(events, issues)
    }

    /** Strict entry point for export: never silently export an incomplete timetable. */
    fun events(item: PortalImport, profile: SchoolStudentProfile? = null): List<SchoolCalendarEvent> {
        require(item.lessons.isNotEmpty()) { "没有课程" }
        val result = resolve(item, profile)
        require(result.issues.isEmpty()) { result.issues.joinToString("；") { it.reason } }
        return result.events
    }

    private fun lessonEvents(item: PortalImport, lesson: PortalLesson, profile: SchoolStudentProfile?): List<SchoolCalendarEvent> {
        val quarter = Regex("timetable-2026-Q([1-4])").matchEntire(item.key.substringAfterLast('/'))
            ?.groupValues?.get(1)?.toInt() ?: error("目前仅核对了 2026 学年度校历")
        require(lesson.day in 1..5 && lesson.period in 1..5) { "该星期或节次尚未适配日历" }
        val code = lesson.description.substringBefore(' ')
        val offering = SchoolCourseOfferings2026.find(code)
            ?: SchoolScienceOfferings2026.find(lesson, quarter, profile?.faculty)
        require(offering?.irregular != true) { "$code 为集中或隔周授课，需核对单独日期" }
        val explicit = Regex("【(前期|前学期|後期|後学期|通年|[1-4]Q)】").findAll(lesson.description)
            .map { it.groupValues[1].replace("前学期", "前期").replace("後学期", "後期") }.toList().distinct()
        require(explicit.size <= 1) { "$code 的学期标记存在冲突" }
        require(explicit.isEmpty() || offering == null || explicit.single() == offering.term) { "$code 的学期标记与官方开课表不一致" }
        val term = explicit.singleOrNull() ?: offering?.term
        val isQuarter = term == "${quarter}Q"
        val isSemester = term == "通年" || term == if (quarter <= 2) "前期" else "後期"
        require(isQuarter.xor(isSemester)) { "无法确认 $code 的授课学季，未生成日期" }
        val campus = offering?.campus ?: when {
            lesson.description.contains("水戸") -> SchoolCampus.MITO
            lesson.description.contains("日立") -> SchoolCampus.HITACHI
            lesson.description.contains("阿見") -> SchoolCampus.AMI
            code.startsWith("KB") -> SchoolCampus.MITO
            profile?.faculty in setOf("人文社会科学部", "教育学部", "理学部", "地域未来共創学環") -> SchoolCampus.MITO
            else -> null
        }
        // Only the third-quarter Friday calendar differs between campuses.
        require(quarter != 3 || lesson.day != 5 || campus != null) { "尚未核对 $code 的校区与授课日期" }
        var courseDates = dates.getValue(quarter)[lesson.day - 1].split(' ')
        if (quarter == 3 && lesson.day == 5) courseDates = when (campus) {
            SchoolCampus.HITACHI -> "09-25 10-02 10-09 10-16 10-23 10-30 11-13".split(' ')
            SchoolCampus.AMI -> "09-25 10-02 10-09 10-16 10-30 11-06 11-13".split(' ')
            else -> courseDates
        }
        if (isSemester) {
            if (quarter == 2 && lesson.day == 2) courseDates = "06-09 06-16 06-23 06-30 07-07 07-14 07-21".split(' ')
            if (quarter == 3 && lesson.day >= 2) courseDates += mapOf(2 to "11-24", 3 to "11-18", 4 to "11-19", 5 to "11-20").getValue(lesson.day)
            if (quarter == 4 && lesson.day >= 2) courseDates = courseDates.dropLast(1)
        }
        return courseDates.mapIndexed { index, date ->
            val year = if (quarter == 4 && date.substringBefore('-').toInt() <= 3) 2027 else 2026
            SchoolCalendarEvent("$year-$date", lesson, index == courseDates.lastIndex && (isQuarter || quarter % 2 == 0))
        }
    }
}

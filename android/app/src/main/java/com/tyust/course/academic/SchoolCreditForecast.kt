package com.tyust.course.academic

import java.math.BigDecimal
import java.text.Normalizer

data class ForecastCourse(val code: String, val name: String, val credits: BigDecimal, val quarter: Int?, val state: String)
data class CreditForecast(val courses: List<ForecastCourse>, val earned: BigDecimal, val missing: List<Int>) {
    val added get() = courses.filter { it.state == "预计新增" }.fold(BigDecimal.ZERO) { s, c -> s + c.credits }
    val total get() = earned + added
    val offered get() = courses.filter { it.state == "预计新增" || it.state == "已修得，不重复增加" }.fold(BigDecimal.ZERO) { s, c -> s + c.credits }
    val failed get() = courses.filter { it.state == "不合格，不计新增" }.fold(BigDecimal.ZERO) { s, c -> s + c.credits }
    val completed get() = courses.filter { it.state == "已修得，不重复增加" }.fold(BigDecimal.ZERO) { s, c -> s + c.credits }
    fun projectedCategories(grade: PortalImport): GraduationProgress = GraduationProgressCalculator.calculate(
        IbarakiCurriculum.requirements, courses.filter { it.state == "预计新增" }.map { c ->
            val known = grade.grades.filter { it.name.trim() == c.name.trim() }.mapNotNull(SchoolGradeClassification::categoryId).distinct()
            val category = known.singleOrNull() ?: IbarakiCurriculum.leaves.singleOrNull { it.label == c.name.trim() }?.id
            AwardedCredit(c.code, c.credits, category)
        })
}

object SchoolCreditForecast {
    private fun name(s: String) = Normalizer.normalize(s.substringBefore("【"), Normalizer.Form.NFKC).replace(Regex("\\s+"), "")
    private fun gradeQuarter(term: String): Int? {
        val value = Normalizer.normalize(term, Normalizer.Form.NFKC)
        return Regex("([1-4])クォーター").find(value)?.groupValues?.get(1)?.toInt()
            ?: when (value.trim()) { "前期" -> 2; "後期", "后期" -> 4; else -> null }
    }
    fun calculate(grade: PortalImport, snapshots: List<PortalImport>, year: Int, quarters: Set<Int>, faculty: String? = null): CreditForecast {
        val owner = grade.key.substringBeforeLast('/', "")
        val tables = snapshots.filter { it.key.substringBeforeLast('/', "") == owner &&
            Regex("timetable-$year-Q[1-4]").matches(it.key.substringAfterLast('/')) }
            .groupBy { it.key }.map { (_, versions) -> versions.maxBy { it.syncedAt } }
        val present = tables.mapNotNull { it.key.substringAfterLast('Q').toIntOrNull() }.toSet()
        // Keep the school's page year/quarter attached to every meeting. A course code
        // can be reused for a later offering; grouping the entire year by code loses that fact.
        val offerings = tables.flatMap { table -> table.lessons.map { table.key.last().digitToInt() to it } }
            .groupBy { (pageQuarter, lesson) ->
                val code = lesson.description.substringBefore(' ')
                val explicit = SchoolAcademicCalendar.creditQuarter(0, code, lesson.description)
                Triple(code, (pageQuarter - 1) / 2, explicit)
            }
        val candidates = offerings.flatMap { (identity, meetings) ->
            val code = identity.first
            val observed = meetings.map { it.first }.toSet()
            val variants = meetings.map { it.second.description }.distinct()
            val d = variants.first()
            val credits = Regex(" ([0-9.]+)単位$").find(d)?.groupValues?.get(1)?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val courseName = d.substringAfter(' ').substringBeforeLast(' ').substringBefore("【").trim()
            val matchingGrades = grade.grades.filter { g ->
                g.year == "$year" && name(g.name) == name(courseName) && g.credits.compareTo(credits) == 0 &&
                    gradeQuarter(g.term) in observed
            }
            // A unique school result can supply the end quarter for other departments/years.
            // Never guess duration from the last timetable currently downloaded.
            val explicit = identity.third
            val resultQuarters = matchingGrades.mapNotNull { gradeQuarter(it.term) }.distinct()
            val namedEnds = if (year == 2026) meetings.mapNotNull { (q, lesson) ->
                SchoolScienceOfferings2026.find(lesson, q, faculty)?.term?.let {
                    when(it) { "前期" -> 2; "後期" -> 4; else -> it.take(1).toIntOrNull() }
                }
            }.distinct().singleOrNull() else null
            val ends = if (explicit != null) listOf(explicit) else if (resultQuarters.isNotEmpty()) resultQuarters
                else listOf((SchoolAcademicCalendar.creditQuarter(year, code, d) ?: namedEnds)?.takeIf { (it - 1) / 2 == identity.second })
            ends.map { q ->
                ForecastCourse(code, courseName, credits, q, if (variants.size != 1 || credits <= BigDecimal.ZERO || q == null) "待核对，不计预览" else "预计新增") to observed
            }
        }
        val courses = candidates.map { it.first }
        val resolved = courses.map { c ->
            if (c.state != "预计新增") c else {
                val matches = grade.grades.filter { g -> g.year == "$year" &&
                    gradeQuarter(g.term) == c.quarter &&
                    name(g.name) == name(c.name) && g.credits.compareTo(c.credits) == 0 }
                val peers = courses.count { name(it.name) == name(c.name) && it.quarter == c.quarter && it.credits.compareTo(c.credits) == 0 }
                c.copy(state = when {
                    matches.isEmpty() -> "预计新增"
                    matches.size != peers || matches.map { it.passed }.distinct().size != 1 -> "成绩对应待核对，不计预览"
                    matches.first().passed -> "已修得，不重复增加"
                    else -> "不合格，不计新增"
                })
            }
        }
        return CreditForecast(resolved.filterIndexed { index, course -> course.quarter in quarters ||
            (course.quarter == null && candidates[index].second.any { it in quarters }) }.sortedWith(compareBy({ it.quarter ?: 5 }, { it.code })),
            grade.grades.filter { it.passed }.fold(BigDecimal.ZERO) { s, g -> s + g.credits }, quarters.filter { it !in present }.sorted())
    }
}

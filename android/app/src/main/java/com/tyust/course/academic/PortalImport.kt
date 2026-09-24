package com.tyust.course.academic

import java.math.BigDecimal

data class PortalGrade(val name: String, val credits: BigDecimal, val score: Int?, val grade: String, val passed: Boolean, val year: String, val term: String, val category: String)
data class PortalLesson(val day: Int, val period: Int, val description: String, val courseName: String = "")
data class PortalImport(val key: String, val title: String, val cards: List<String>,
    val grades: List<PortalGrade> = emptyList(), val lessons: List<PortalLesson> = emptyList(), val syncedAt: Long = 0)

/** Parse only the two observed school layouts; ambiguous pages never replace stored data. */
object PortalImportParser {
    fun parse(text: String, allowEmptyTimetable: Boolean = false, courseNames: Map<String, String> = emptyMap()): PortalImport {
        val lines = text.lines().map(String::trim)
        val gradeHeader = lines.indexOfFirst { it.startsWith("No. | 科目大区分 |") }
        if (gradeHeader >= 0) {
            val rows = lines.drop(gradeHeader + 1).takeWhile { it.isNotBlank() }
            val grades = rows.map { line ->
                val c = line.split(" | ")
                require(c.size == 11 && c[0].toIntOrNull() != null) { "成绩表结构变化，未保存" }
                val credits = c[5].toBigDecimalOrNull()
                require(credits != null && credits > BigDecimal.ZERO && credits <= BigDecimal.TEN) { "学分无法识别" }
                require(c[10] in listOf("合", "否")) { "包含未确定成绩，未保存" }
                val score = c[8].toIntOrNull()
                require(score == null || score in 0..100) { "分数超出范围" }
                require(score == null || (score >= 60) == (c[10] == "合")) { "分数与合否不一致，未保存" }
                PortalGrade(c[4], credits, score, c[9], c[10] == "合", c[6], c[7], c.subList(1, 4).filter(String::isNotBlank).joinToString(" / "))
            }
            require(grades.isNotEmpty()) { "成绩为空，保留原记录" }
            val earned = grades.filter { it.passed }.fold(BigDecimal.ZERO) { sum, g -> sum + g.credits }
            val reported = Regex("修得単位数 \\| ([0-9.]+)").find(text)?.groupValues?.get(1)?.toBigDecimalOrNull()
            require(reported != null && earned.compareTo(reported) == 0) { "明细学分与学校总数不一致，请显示全部成绩后读取" }
            val gpaHeader = lines.indexOfFirst { it == "年度・学期 | 学期GPA | 年間GPA | 通算GPA" }
            val gpa = if (gpaHeader >= 0) lines.getOrNull(gpaHeader + 1)?.split(" | ")?.takeIf { it.size == 4 }?.last() else null
            return PortalImport("grades", "学校成绩 · ${grades.size} 门 · 已修 $earned 学分", listOf("学校通算 GPA：${gpa ?: "未提供"}（直接采用学校显示值）") + grades.map {
                "${it.name}\n${it.credits} 学分 · ${it.score?.toString() ?: "无百分制分数"} · ${it.grade} · ${if (it.passed) "通过" else "未通过"}\n${it.year} · ${it.term}\n${it.category}"
            }, grades = grades)
        }
        val term = Regex("年度・学期 \\| (\\d{4})年度 ([1-4])クォーター").find(text)
            ?: error("当前页尚未适配，请打开学分成绩明细或学季课表")
        require(lines.any { it == "| 月曜日 | 火曜日 | 水曜日 | 木曜日 | 金曜日 | 土曜日" }) { "课表星期结构变化，未保存" }
        val lessons = mutableListOf<PortalLesson>()
        for (period in 1..6) {
            val start = lines.indexOf("${period}限")
            require(start >= 0) { "课表节次不完整，未保存" }
            val end = if (period < 6) lines.indexOf("${period + 1}限") else lines.indexOfFirst {
                it == "集中講義など" || it == "集中講義など | 集中講義を登録"
            }
            require(end > start) { "课表节次顺序异常" }
            // During registration the school adds an action row below an existing course.
            // It is not a seventh weekday or another lesson; retain all other validation.
            val cells = lines.subList(start + 1, end).filter { it.isNotBlank() && it != "追加登録" }
            require(cells.size == 6) { "课表列数变化，无法确认星期位置" }
            cells.forEachIndexed { day, cell ->
                if (cell != "未登録") {
                    require(Regex("^[A-Z][A-Za-z0-9]+(?:-[A-Za-z0-9]+)* .+ [0-9.]+単位$").matches(cell)) { "存在未识别课程，未保存" }
                    val exactName=courseNames[cell.substringBefore(' ')]?.takeIf {name ->
                        name.isNotBlank() && cell.substringAfter(' ').startsWith("$name ")
                    }.orEmpty()
                    lessons += PortalLesson(day + 1, period, cell, exactName)
                }
            }
        }
        require(text.contains("登録されていません")) { "包含集中授课，需进一步适配" }
        val count = Regex("件数 \\| (\\d+)件").find(text)?.groupValues?.get(1)?.toIntOrNull()
        require((allowEmptyTimetable || lessons.isNotEmpty()) && lessons.size == count) { "读取课程数与学校不一致，未保存" }
        val year = term.groupValues[1]; val q = term.groupValues[2]
        return PortalImport("timetable-$year-Q$q", "$year · Q$q 课表 · ${lessons.size} 门", listOf("仅保存此学季的星期与节次；尚未设置上课日期，不生成日历事件。") + lessons.sortedWith(compareBy({ it.day }, { it.period })).map {
            "星期${listOf("一", "二", "三", "四", "五", "六")[it.day - 1]} · 第 ${it.period} 限\n${it.description}"
        }, lessons = lessons)
    }
}

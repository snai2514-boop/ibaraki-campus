package com.tyust.course.academic

/** Reviewed 2026 syllabus decisions. Never infer online eligibility from 'blended' alone. */
object SchoolCourseVenue {
    private val online2026 = setOf("KB7003", "KB9304", "KB9307", "KB9420", "KB9422")
    fun deliveryLabel(delivery: String): String? {
        if(delivery.isBlank()) return null
        val value=delivery.lowercase()
        if(listOf("blended", "hybrid", "ブレンド", "混合", "ハイブリッド", "併用").any(value::contains)) return "混合授课"
        if(value.contains("対面") || value.contains("face-to-face")) {
            return if(value.contains("オンライン") || value.contains("on-line") || value.contains("online")) "混合授课" else "线下授课"
        }
        if(value.contains("オンライン") || value.contains("on-line") || value.contains("online")) {
            return when {
                value.contains("オンデマンド") || value.contains("on demand") || value.contains("on-demand") -> "线上授课（录播）"
                value.contains("リアルタイム") || value.contains("real time") || value.contains("real-time") -> "线上授课（实时）"
                else -> "线上上课"
            }
        }
        return null
    }
    fun label(key: String, description: String, classroom: String = "", delivery: String = ""): String {
        val code = description.substringBefore(' ')
        // These reviewed exceptions apply to a particular cohort or individual sessions.
        val exception=key.substringAfterLast('/').startsWith("timetable-2026-") && code in setOf("KB4012", "KB2004")
        if(!exception) deliveryLabel(delivery)?.let { mode ->
            return when(mode) {
                "线下授课" -> classroom.ifBlank { "线下授课 · 教室待确认" }
                "混合授课" -> "混合授课 · ${classroom.ifBlank { "按每次通知" }}"
                else -> mode
            }
        }
        if (key.substringAfterLast('/').startsWith("timetable-2026-") && code in online2026) return "线上上课"
        return classroom.ifBlank { "教室待确认" }
    }
    fun explanation(key: String, description: String): String {
        if (!key.substringAfterLast('/').startsWith("timetable-2026-")) return "尚未核实本年度授课方式。"
        return when (description.substringBefore(' ')) {
            "KB4012" -> "一年级学生在水户面授；线上参加仅适用于日立校区重修生。"
            "KB2004" -> "各次授课的面授/线上方式由讲师另行通知，优先按教室安排。"
            in online2026 -> "2026 年学校课程大纲列为线上授课；临时调整以教师通知为准。"
            else -> "教室以学校当前安排为准；未明确核实的线上方式不自动采用。"
        }
    }
}

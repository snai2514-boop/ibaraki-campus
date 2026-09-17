package com.tyust.course.academic

object SyllabusSummary {
    fun exam(grading: String, plan: String): String {
        val relevant=(grading+"。"+plan).split(Regex("[。\\n]"))
            .filter { Regex("期末|学期末|定期試験|final\\s+exam",RegexOption.IGNORE_CASE).containsMatchIn(it) }
        return relevant.joinToString("。 ").trim().ifBlank { "学校未明确注明，请查看评分方法与授课计划" }
    }
}

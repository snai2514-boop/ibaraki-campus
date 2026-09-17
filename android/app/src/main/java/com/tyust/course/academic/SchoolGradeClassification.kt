package com.tyust.course.academic

import java.text.Normalizer

/** 2026 Information Engineering. Match both course and school category for broad groups.
 * General education guide: Humanities / Nature, Environment and Humans.
 * Engineering registration guide printed p22: the two foundation courses are mandatory, not A electives.
 */
object SchoolGradeClassification {
    private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC).trim()

    fun categoryId(grade: PortalGrade): String? {
        val category = normalized(grade.category.substringAfterLast(" / "))
        IbarakiCurriculum.leaves.singleOrNull { normalized(it.label) == category }?.let { return it.id }
        val name = normalized(grade.name)
        return when {
            category == "多文化理解" && name == "思想・文学" -> "culture_humanities"
            category == "自然と社会の広がり" && name == "技術と社会" -> "nature"
            category == "学科共通専門基礎科目-必修" &&
                name in setOf("ソフトウェア基礎", "コンピュータ基礎") -> "department_core"
            else -> null
        }
    }
}

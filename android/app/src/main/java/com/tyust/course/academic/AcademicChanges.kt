package com.tyust.course.academic

import java.math.BigDecimal

object AcademicChanges {
    fun gpa(item: PortalImport): BigDecimal? = Regex("GPA[：:]\\s*([0-9]+(?:\\.[0-9]+)?)")
        .find(item.cards.firstOrNull().orEmpty())?.groupValues?.get(1)?.toBigDecimalOrNull()
    fun earned(item: PortalImport) = item.grades.filter { it.passed }.fold(BigDecimal.ZERO) { sum, g -> sum + g.credits }
    fun changes(old: PortalImport?, current: PortalImport): List<String> {
        if (old == null || old.key != current.key || old.grades.isEmpty() || current.grades.isEmpty()) return emptyList()
        fun n(v: BigDecimal) = v.stripTrailingZeros().toPlainString()
        return buildList {
            val before = earned(old); val after = earned(current)
            if (after > before) add("已修学分增加：${n(before)} → ${n(after)}（+${n(after-before)}）")
            val previousGpa = gpa(old); val nextGpa = gpa(current)
            if (previousGpa != null && nextGpa != null && previousGpa.compareTo(nextGpa) != 0)
                add("GPA 变化：${n(previousGpa)} → ${n(nextGpa)}")
        }
    }
}

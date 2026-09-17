package com.tyust.course.academic

import java.math.BigDecimal

/** Requirements must come from the student's applicable official curriculum. */
data class CreditRequirement(
    val id: String,
    val label: String,
    val required: BigDecimal,
    val children: List<CreditRequirement> = emptyList()
)

/** One awarded course, assigned to one leaf category; unassigned credits stay visible. */
data class AwardedCredit(val courseId: String, val credits: BigDecimal, val categoryId: String?)

data class CreditProgress(
    val requirement: CreditRequirement,
    val earned: BigDecimal,
    val children: List<CreditProgress>
) {
    val remaining: BigDecimal get() = (requirement.required - earned).max(BigDecimal.ZERO)
    val satisfied: Boolean get() = remaining.signum() == 0 && children.all { it.satisfied }
    // Graduation minimums are not registration caps. Zero means no individual minimum.
    val display: String get() = if (requirement.required.signum() > 0)
        "${earned.stripTrailingZeros().toPlainString()}/${requirement.required.stripTrailingZeros().toPlainString()}"
    else "已修 ${earned.stripTrailingZeros().toPlainString()} 学分"
}

data class GraduationProgress(val categories: List<CreditProgress>, val unassigned: BigDecimal)

object GraduationProgressCalculator {
    fun calculate(requirements: List<CreditRequirement>, courses: List<AwardedCredit>): GraduationProgress {
        val ids = mutableSetOf<String>()
        val leaves = mutableSetOf<String>()
        fun validate(node: CreditRequirement) {
            require(node.id.isNotBlank() && ids.add(node.id)) { "Requirement IDs must be unique and nonblank" }
            require(node.required.signum() >= 0) { "Required credits cannot be negative" }
            if (node.children.isEmpty()) leaves.add(node.id)
            node.children.forEach(::validate)
        }
        requirements.forEach(::validate)
        require(courses.map { it.courseId }.distinct().size == courses.size) { "Resolve repeated courses before counting awarded credits" }
        courses.forEach {
            require(it.courseId.isNotBlank() && it.credits.signum() >= 0)
            require(it.categoryId == null || it.categoryId in leaves) { "Credits must be assigned to an existing leaf category" }
        }
        fun progress(node: CreditRequirement): CreditProgress {
            val children = node.children.map(::progress)
            val earned = if (children.isEmpty()) courses.filter { it.categoryId == node.id }
                .fold(BigDecimal.ZERO) { sum, course -> sum + course.credits }
            else children.fold(BigDecimal.ZERO) { sum, child -> sum + child.earned }
            return CreditProgress(node, earned, children)
        }
        return GraduationProgress(requirements.map(::progress), courses.filter { it.categoryId == null }
            .fold(BigDecimal.ZERO) { sum, course -> sum + course.credits })
    }
}

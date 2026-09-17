package com.tyust.course.academic

import java.math.BigDecimal
import java.math.RoundingMode

/** 2026 entrants, Information Engineering, regular graduation. Printed pages 3, 20, 49-50. */
object IbarakiCurriculum {
    const val sourceUrl = "https://www.eng.ibaraki.ac.jp/common/education/class/2026-course-registration03.pdf"
    const val guidanceUrl = "https://www.eng.ibaraki.ac.jp/education/class/"
    private fun r(id: String, label: String, amount: Int, vararg children: CreditRequirement) =
        CreditRequirement(id, label, amount.toBigDecimal(), children.toList())
    val requirements = listOf(r("total", "毕业学分总计", 124,
        r("general", "基盘教育", 24,
            r("foundation", "基盘学修", 14,
                r("seminar", "大学入門ゼミ", 2), r("ibaraki", "茨城学", 1),
                r("english", "プラクティカル・イングリッシュ", 4), r("literacy", "情報リテラシー", 2),
                r("data_ai", "データサイエンス・AI入門", 2), r("health", "心と体の健康", 1),
                r("ethics", "科学と倫理", 1), r("life", "ライフデザイン", 1)),
            r("liberal", "主体学修", 7,
                r("culture", "多文化理解", 3,
                    r("culture_language", "多文化コミュニケーション（初修外国語）", 0),
                    r("culture_communication", "共生とコミュニケーション", 0),
                    r("culture_arts", "パフォーマンス＆アート", 0),
                    r("culture_humanities", "ヒューマニティーズ", 0)),
                r("nature_society", "自然と社会の広がり", 4,
                    r("nature", "自然・環境と人間", 0), r("society", "グローバル化と人間社会", 0))),
            r("general_elective", "基盘教育・选择履修", 3)),
        r("professional", "专业科目", 92,
            r("faculty_core", "学部共通専門基礎教育科目", 16),
            r("required", "专业必修", 55,
                r("department_core", "学科共通専門基礎（A 科目除外）", 33),
                r("cross_program", "プログラム横断科目", 14), r("thesis", "卒業研究", 8)),
            r("professional_elective", "专业选择必修", 21)),
        r("free", "自由履修", 8)))
    val leaves: List<CreditRequirement> = buildList {
        fun visit(node: CreditRequirement) { if (node.children.isEmpty()) add(node) else node.children.forEach(::visit) }
        requirements.forEach(::visit)
    }
}

enum class StudyGradeKind(val label: String) {
    SCORED("百分制成绩"), RECOGNIZED("认定学分（不计 GPA）"),
    INTERNSHIP("学外实习（不计 GPA）"), OUTSIDE("毕业要求外（不计 GPA）"),
    WITHDRAWN("已获准取消履修"), PENDING("在修／成绩待定")
}

/** One current result per course. A retake replaces the prior result for cumulative GPA. */
data class StudyRecord(
    val courseId: String, val name: String, val credits: BigDecimal,
    val score: Int?, val kind: StudyGradeKind, val categoryId: String?
) {
    fun validate() {
        require(courseId.isNotBlank() && name.isNotBlank()) { "请填写课程代码和名称" }
        require(credits > BigDecimal.ZERO && credits <= BigDecimal("100")) { "学分须大于 0 且不超过 100" }
        require(score == null || score in 0..100) { "分数须为 0–100 的整数" }
        require(kind != StudyGradeKind.SCORED || score != null) { "请填写百分制成绩；未公布成绩请选择在修" }
        require(categoryId == null || IbarakiCurriculum.leaves.any { it.id == categoryId }) { "课程分类无效" }
    }
    val awarded: Boolean get() = when (kind) {
        StudyGradeKind.SCORED -> score != null && score >= 60
        StudyGradeKind.RECOGNIZED, StudyGradeKind.INTERNSHIP -> true
        else -> false
    }
}

object IbarakiStudyCalculator {
    /** Category advice only, until current offerings/prerequisites and the timetable are available. */
    fun suggestions(records: List<StudyRecord>): List<CreditProgress> = buildList {
        fun visit(node: CreditProgress) {
            if (node.requirement.required.signum() > 0 &&
                (node.children.isEmpty() || node.children.all { it.requirement.required.signum() == 0 })) {
                if (node.remaining.signum() > 0) add(node)
            } else node.children.forEach(::visit)
        }
        progress(records).categories.forEach(::visit)
    }
    fun validate(records: List<StudyRecord>) {
        records.forEach { it.validate() }
        require(records.map { it.courseId }.distinct().size == records.size) { "课程代码重复，请编辑已有课程的最新成绩" }
    }
    fun gpa(records: List<StudyRecord>): BigDecimal? {
        validate(records)
        val scored = records.filter { it.kind == StudyGradeKind.SCORED }
        val credits = scored.fold(BigDecimal.ZERO) { sum, course -> sum + course.credits }
        if (credits.signum() == 0) return null
        val points = scored.fold(BigDecimal.ZERO) { sum, course ->
            val gp = if (course.score!! < 60) BigDecimal.ZERO else (course.score - 55).toBigDecimal().divide(BigDecimal.TEN)
            sum + gp * course.credits
        }
        return points.divide(credits, 2, RoundingMode.HALF_UP)
    }
    fun progress(records: List<StudyRecord>): GraduationProgress {
        validate(records)
        return GraduationProgressCalculator.calculate(IbarakiCurriculum.requirements,
            records.filter { it.awarded }.map { AwardedCredit(it.courseId, it.credits, it.categoryId) })
    }
}

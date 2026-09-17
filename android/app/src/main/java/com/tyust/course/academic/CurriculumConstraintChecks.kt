package com.tyust.course.academic

import java.math.BigDecimal

data class CurriculumCheckSpec(val id: String, val title: String, val minimum: Int,
    val categories: List<String> = emptyList(), val names: List<String> = emptyList())
data class CurriculumCheckResult(val spec: CurriculumCheckSpec, val earned: BigDecimal, val status: String, val note: String) {
    val remaining get() = (spec.minimum.toBigDecimal() - earned).max(BigDecimal.ZERO)
}

/** Only awarded credits support minimum checks. Forecasts and approvals never count as earned. */
object CurriculumConstraintChecks {
    fun specs(s: CurriculumScope): List<CurriculumCheckSpec> {
        if (CurriculumConstraints.rules(s).isEmpty()) return emptyList()
        val human = s.faculty == "人文社会科学部"
        val education = s.faculty == "教育学部"
        val future = s.faculty == "地域未来共創学環"
        val specs = mutableListOf<CurriculumCheckSpec>()
        fun category(id: String, title: String, minimum: Int, vararg ids: String) {
            specs += CurriculumCheckSpec(id, title, minimum, ids.toList())
        }
        category("seminar", "大学入門ゼミ", 2, "seminar")
        category("ibaraki", "茨城学", 1, "ibaraki")
        category("english", "プラクティカル・イングリッシュ", 4, "english")
        category("literacy", "情報リテラシー", 2, "literacy")
        category("ai", "データサイエンス・AI入門", 2, "data_ai")
        specs += CurriculumCheckSpec("physical", "身体活動", if (education) 2 else 1, names = listOf("身体活動", "心と体の健康（身体活動）"))
        if (!human) category("ethics", "科学と倫理", 1, "ethics")
        if (!future) category("life", "ライフデザイン", 1, "life")
        category("culture", "多文化理解（合计）", if (human) 2 else 3,
            "culture_language", "culture_communication", "culture_arts", "culture_humanities")
        category("nature_society", "自然と社会の広がり（合计）", if (future) 6 else 4, "nature", "society")
        if (future) {
            category("communication", "多文化コミュニケーション（选择组合）", 1, "culture_language", "culture_communication", "culture_arts")
            category("humanities", "ヒューマニティーズ", 2, "culture_humanities")
            category("nature", "自然・環境と人間", 2, "nature")
            category("society", "グローバル化と人間社会", 4, "society")
            for (suffix in listOf("Ⅰ", "Ⅱ")) specs += CurriculumCheckSpec("entrepreneurship_$suffix", "アントレプレナーシップ入門$suffix", 1,
                names = listOf("アントレプレナーシップ入門$suffix"))
        }
        if (education) specs += CurriculumCheckSpec("constitution", "日本国憲法（必修）", 2, names = listOf("日本国憲法"))
        if (s.faculty == "工学部" && s.department == "機械システム工学科" && s.cohort == 2026)
            specs += CurriculumCheckSpec("environment", "環境と人間（必修）", 1, names = listOf("環境と人間"))
        if (s.faculty == "理学部" && s.cohort == 2026 && (s.program.contains("地球科学技術者養成") || s.program.contains("E-J"))) {
            category("nature", "自然・環境と人間（指定组合）", 2, "nature")
            category("society", "グローバル化と人間社会（指定组合）", 2, "society")
        }
        return specs
    }

    fun evaluate(s: CurriculumScope, grades: List<PortalGrade>?, specs: List<CurriculumCheckSpec> = specs(s)): List<CurriculumCheckResult> {
        val awarded = grades.orEmpty().filter { it.passed && it.credits.signum() > 0 }
        fun excluded(g: PortalGrade) = g.category.split('/').map(UniversityCourseClassification::normalize).any { it.contains("卒業要件外") || it.contains("専門") || it.contains("自由履修") }
        val knownNames = specs.flatMap { it.names }.map(UniversityCourseClassification::normalize).toSet()
        val unresolved = awarded.filter { !excluded(it) && UniversityCourseClassification.generalCategory(it.name, it.category) == null && UniversityCourseClassification.normalize(it.name) !in knownNames }
            .fold(BigDecimal.ZERO) { total, g -> total + g.credits }
        return specs.map { spec ->
            val earned = awarded.filter { g ->
                val path = g.category.split('/').map(UniversityCourseClassification::normalize)
                if (path.any { it.contains("卒業要件外") || it.contains("専門") || it.contains("自由履修") }) false
                else if (spec.names.isNotEmpty()) spec.names.any { n ->
                    UniversityCourseClassification.normalize(g.name) == UniversityCourseClassification.normalize(n) || UniversityCourseClassification.normalize(n) in path
                } else UniversityCourseClassification.generalCategory(g.name, g.category) in spec.categories
            }.fold(BigDecimal.ZERO) { sum, g -> sum + g.credits }
            val missing = (spec.minimum.toBigDecimal() - earned).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
            val status = when { grades == null -> "尚未同步"; earned >= spec.minimum.toBigDecimal() -> "已满足学分数"; unresolved.signum() > 0 -> "待核对分类"; else -> "已保存成绩尚不足" }
            val note = when (status) {
                "尚未同步" -> "尚无学校成绩数据，不能判断是否满足。"
                "待核对分类" -> "当前确认学分距最低值还差 $missing；另有 ${unresolved.stripTrailingZeros().toPlainString()} 学分归类未确认，核对后可能改变结果。"
                "已保存成绩尚不足" -> "按已保存的合格成绩还差 $missing 学分；在修或待出成绩课程尚未计入，这不表示规则无法确认。"
                else -> "仅确认学分数；审批、指定班级等其他条件另行核对。"
            }
            CurriculumCheckResult(spec, earned, status, note)
        }
    }
}

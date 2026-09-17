package com.tyust.course.academic

import java.text.Normalizer
import java.math.BigDecimal

/** A rule belongs to a curriculum cohort, not the year a particular class is taken. */
data class CourseCategoryRule(val cohort: Int, val faculty: String, val department: String,
    val program: String, val name: String, val category: String, val source: String, val page: Int,
    val code: String = "", val credits: BigDecimal? = null)

class UniversityCourseCatalog(val rules: List<CourseCategoryRule>) {
    private val byName = rules.groupBy { UniversityCourseClassification.normalize(it.name) }
    fun find(scope: CurriculumScope, name: String, code: String = "", credits: BigDecimal? = null): List<CourseCategoryRule> {
        val scoped = byName[UniversityCourseClassification.normalize(name)].orEmpty().filter {
        it.cohort == scope.cohort && it.faculty == scope.faculty &&
            (it.department == "*" || it.department == scope.department) &&
            (it.program == "*" || it.program == scope.program) &&
            (credits == null || it.credits == null || credits.compareTo(it.credits) == 0)
        }
        if (code.isBlank()) return scoped
        val exact = scoped.filter { it.code.equals(code, ignoreCase = true) }
        return exact.ifEmpty { scoped.filter { it.code.isBlank() } }
    }
    companion object {
        val empty = UniversityCourseCatalog(emptyList())
        fun parse(text: String): UniversityCourseCatalog = UniversityCourseCatalog(text.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
                val c = line.split('\t')
                require(c.size == 8 || c.size == 10) { "Invalid course classification row" }
                CourseCategoryRule(c[0].toInt(), c[1], c[2], c[3], c[4], c[5], c[6], c[7].toInt(),
                    c.getOrElse(8) { "" }, c.getOrNull(9)?.takeIf { it.isNotBlank() }?.toBigDecimal())
            }.toList())
    }
}

data class CourseClassification(val categoryId: String?, val label: String, val source: String = "", val excluded: Boolean = false)

/** Classify only within the student's selected faculty, department, program and cohort. */
object UniversityCourseClassification {
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .replace(Regex("\\s+"), "").replace(Regex("(?<=[ァ-ヶ])[-−](?=[ァ-ヶ])"), "ー")
    private val general = mapOf(
        "seminar" to listOf("大学入門ゼミ"), "ibaraki" to listOf("茨城学"),
        "english" to listOf("プラクティカル・イングリッシュ", "Integrated English", "Communicative English"),
        "literacy" to listOf("情報リテラシー"), "data_ai" to listOf("データサイエンス・AI入門"),
        "health" to listOf("心と体の健康", "心と体の健康（身体活動）", "身体活動", "健康の科学"),
        "ethics" to listOf("科学と倫理"), "life" to listOf("ライフデザイン"),
        "culture_language" to listOf("多文化コミュニケーション（初修外国語）", "多文化コミュニケーション", "ドイツ語入門", "フランス語入門", "中国語入門", "朝鮮語入門", "スペイン語入門", "学術日本語I", "学術日本語IIA", "学術日本語IIB", "学術日本語IIC"),
        "culture_communication" to listOf("共生とコミュニケーション"),
        "culture_arts" to listOf("パフォーマンス＆アート"),
        "culture_humanities" to listOf("ヒューマニティーズ", "思想・文学", "歴史・考古学", "人間科学", "メディア文化"),
        "nature" to listOf("自然・環境と人間", "物質と生命", "技術と社会", "環境と人間"),
        "society" to listOf("グローバル化と人間社会", "法律・政治", "経済・経営", "日本国憲法", "公共社会", "グローバル・スタディーズ")
    )
    fun requirements(scope: CurriculumScope): List<CreditRequirement> {
        val summary = UniversityCurricula.summary(scope) ?: return emptyList()
        if (summary.detailedInformation2026) return IbarakiCurriculum.requirements
        return listOf(CreditRequirement("total", "毕业学分总计", summary.total.toBigDecimal(),
            summary.rows.mapIndexed { i, (label, credits) -> CreditRequirement("category_$i", label, credits.toBigDecimal()) }))
    }
    private fun leaves(nodes: List<CreditRequirement>): List<CreditRequirement> = nodes.flatMap {
        if (it.children.isEmpty()) listOf(it) else leaves(it.children)
    }
    private fun target(scope: CurriculumScope, category: String): CreditRequirement? {
        val all = leaves(requirements(scope))
        all.singleOrNull { it.id == category || normalize(it.label) == normalize(category) }?.let { return it }
        val n = normalize(category)
        val alias = when (scope.faculty) {
            "工学部" -> when (n) {
                "学部共通専門基礎教育科目", "学部共通専門基礎科目" -> "専門必修"
                "必修", "専門必修科目", "学科共通専門基礎科目-必修" -> "専門必修"
                "選択必修", "選択必修科目", "専門選択必修科目", "学科共通専門基礎科目-選択必修" -> "専門選択必修"
                else -> null
            }
            "理学部" -> when (n) {
                "基礎科目" -> "専門基礎科目"
                "標準科目" -> if (scope.program == "生物科学コース") "専門標準・発展科目" else "専門標準科目"
                "発展科目" -> if (scope.program == "生物科学コース") "専門標準・発展科目" else "専門発展科目"
                "標準・発展科目" -> "専門標準・発展科目"
                else -> null
            }
            else -> null
        }
        all.singleOrNull { it.label == alias }?.let { return it }
        if (scope.faculty in setOf("教育学部", "農学部", "地域未来共創学環") &&
            n in setOf("専門科目", "教科に関する科目", "教育の基礎的理解に関する科目", "養護に関する科目",
                "教育実践に関する科目", "卒業研究", "学環基盤科目", "プログラム共通科目", "プログラムコア科目", "課題探究科目", "コーオプ実習")) {
            return all.singleOrNull { it.label == "専門科目" }
        }
        if (scope.faculty == "理学部" && scope.program in setOf("総合理学コース", "学際理学コース") &&
            n in setOf("基礎科目", "標準科目", "発展科目", "標準・発展科目")) return all.singleOrNull { it.label == "専門科目" }
        if (n in setOf("自由履修科目", "自由履修")) return all.singleOrNull { it.id == "free" || it.label.startsWith("自由履修") }
        return null
    }
    fun classify(scope: CurriculumScope, name: String, schoolCategory: String = "", catalog: UniversityCourseCatalog = UniversityCourseCatalog.empty,
        code: String = "", credits: BigDecimal? = null): CourseClassification {
        val summary = UniversityCurricula.summary(scope) ?: return CourseClassification(null, "待分类")
        val path = schoolCategory.split(Regex("\\s*/\\s*")).map(::normalize)
        if (path.any { it in setOf("卒業要件外", "卒業要件外科目", "卒業要件に算入しない科目") }) return CourseClassification(null, "毕业要求外", excluded = true)
        val rows = catalog.find(scope, name, code, credits)
        // Exact school categories outrank catalog predictions, including individual recognition.
        path.asReversed().firstNotNullOfOrNull { target(scope, it) }?.let { return CourseClassification(it.id, it.label, "学校成绩分类") }
        if (rows.isNotEmpty() && rows.all { it.category == "outside" }) return CourseClassification(null, "毕业要求外", rows.first().source, true)
        val matches = rows.mapNotNull { target(scope, it.category) }.distinctBy { it.id }
        if (matches.size == 1 && rows.none { it.category == "outside" }) return CourseClassification(matches.single().id, matches.single().label, "${rows.first().source}#page=${rows.first().page}")
        val generalId = general.entries.singleOrNull { (_, names) -> names.any { normalize(it) == normalize(name) || normalize(it) in path } }?.key
            ?: "english".takeIf { Regex("(?:IntegratedEnglish[1-3][ABCD]|CommunicativeEnglish[1-3][AB])").matches(normalize(name)) }
        if (generalId != null && (schoolCategory.isBlank() || path.none { it.contains("専門") || it.contains("自由履修") })) {
            val node = if (summary.detailedInformation2026) IbarakiCurriculum.leaves.single { it.id == generalId }
                else leaves(requirements(scope)).single { it.label == "基盤教育科目" }
            return CourseClassification(node.id, node.label, "大学共通教育履修案内")
        }
        return CourseClassification(null, "待分类")
    }
    fun progress(scope: CurriculumScope, grade: PortalImport, catalog: UniversityCourseCatalog): GraduationProgress =
        GraduationProgressCalculator.calculate(requirements(scope), grade.grades.mapIndexedNotNull { i, g ->
            if (!g.passed) null else classify(scope,g.name,g.category,catalog,credits=g.credits).let { c ->
                if (c.excluded) null else AwardedCredit("${grade.key}/$i", g.credits, c.categoryId)
            }
        })
    fun classifyForecast(scope: CurriculumScope, c: ForecastCourse, grade: PortalImport, catalog: UniversityCourseCatalog): CourseClassification {
        val direct = classify(scope, c.name, catalog = catalog, code = c.code, credits = c.credits)
        // A student's explicit school recognition outranks the generic catalog; conflicting recognitions stay pending.
        val previous = grade.grades.filter { it.category.isNotBlank() && normalize(it.name) == normalize(c.name) && it.credits.compareTo(c.credits) == 0 }
            .map { classify(scope,it.name,it.category,catalog,credits=it.credits) }
            .filter { it.categoryId != null || it.excluded }.distinctBy { it.categoryId to it.excluded }
        return when (previous.size) { 0 -> direct; 1 -> previous.single(); else -> CourseClassification(null, "待分类") }
    }
    fun forecast(scope: CurriculumScope, forecast: CreditForecast, grade: PortalImport, catalog: UniversityCourseCatalog): GraduationProgress =
        GraduationProgressCalculator.calculate(requirements(scope), forecast.courses.filter { it.state == "预计新增" }.mapIndexedNotNull { i,c ->
            val classification = classifyForecast(scope, c, grade, catalog)
            if (classification.excluded) null else AwardedCredit("${c.code}/${c.quarter}/$i",c.credits,classification.categoryId)
        })
}

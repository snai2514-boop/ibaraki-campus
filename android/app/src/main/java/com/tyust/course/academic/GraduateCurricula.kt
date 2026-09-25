package com.tyust.course.academic

import java.math.BigDecimal

enum class StudyLevel(val label: String) {
    UNDERGRADUATE("本科"), MASTER("硕士／博士前期"), DOCTOR("博士／博士后期"),
    PROFESSIONAL("专业职学位（教职）"), NON_DEGREE("非学位研究生等"), UNKNOWN("大学院层次待确认")
}

data class GraduateRule(val total: Int, val requirements: List<CreditRequirement>, val source: String,
    val conditions: List<String>, val notes: String, val standardYears: Int)

/** Verified 2026 graduate guides. No undergraduate defaults, no inferred thesis completion. */
object GraduateCurricula {
    const val HUMAN = "人文社会科学研究科"
    const val SCIENCE = "理工学研究科"
    const val EDUCATION = "教育学研究科"
    const val AGRICULTURE = "農学研究科"
    const val UNITED = "連合農学研究科（東京農工大学・茨城大学配置）"
    const val MASTER_SOURCE = "https://www.gse.ibaraki.ac.jp/common/collegelife/master/subjects/2026_subjects06.pdf#page=19"
    const val SCIENCE_SOURCE = "https://www.gse.ibaraki.ac.jp/common/collegelife/master/subjects/2026_subjects04.pdf#page=19"
    const val DOCTOR_SOURCE = "https://www.gse.ibaraki.ac.jp/common/collegelife/gs_doctor/subjects/2026_subjects05.pdf"
    const val HUMAN_SOURCE = "https://www.hum.ibaraki.ac.jp/pdf/daigakuin/gr_handbook.pdf#page=29"
    const val EDUCATION_SOURCE = "https://www.ppedu.ibaraki.ac.jp/images/2026pamphlet.pdf"
    const val AGRICULTURE_SOURCE = "https://www.agr.ibaraki.ac.jp/graduate/relation/files/r8_academic_guide.pdf#page=46"
    const val UNITED_SOURCE = "https://web.tuat.ac.jp/~kisoku/act/110000187.html"
    val departments = linkedMapOf(
        HUMAN to listOf("人文科学専攻", "社会科学専攻"),
        SCIENCE to listOf("理学専攻", "量子線科学専攻", "機械システム工学専攻", "電気電子システム工学専攻", "情報工学専攻", "都市システム工学専攻", "複合新領域科学専攻", "社会インフラシステム科学専攻"),
        EDUCATION to listOf("教育実践高度化専攻"),
        AGRICULTURE to listOf("農学専攻"),
        UNITED to listOf("生物生産科学専攻", "応用生命科学専攻", "環境資源共生科学専攻", "農業環境工学専攻", "農林共生社会科学専攻")
    )
    val doctorMajors = listOf("量子線科学専攻", "複合新領域科学専攻", "社会インフラシステム科学専攻")
    val educationCourses = listOf("学校運営コース", "教育方法開発コース", "児童生徒支援コース", "教科領域コース", "特別支援科学コース", "養護科学コース")
    fun isGraduate(s: CurriculumScope) = s.level != StudyLevel.UNDERGRADUATE || s.faculty.contains("研究科") || s.faculty.contains("大学院")
    fun faculty(affiliation: String): String? = when {
        affiliation.contains("連合農学研究科") -> UNITED
        else -> departments.keys.firstOrNull { affiliation.contains(it) }
    }
    fun detectLevel(text: String): StudyLevel = when {
        listOf("研究生", "科目等履修生", "特別聴講学生", "特別研究学生", "非正規生").any { text.contains(it) } -> StudyLevel.NON_DEGREE
        text.contains("専門職") || text.contains("教職大学院") -> StudyLevel.PROFESSIONAL
        text.contains("博士後期") -> StudyLevel.DOCTOR
        text.contains("博士前期") || text.contains("修士課程") -> StudyLevel.MASTER
        text.contains("博士課程") -> StudyLevel.DOCTOR
        text.contains("研究科") || text.contains("大学院") -> StudyLevel.UNKNOWN
        else -> StudyLevel.UNDERGRADUATE
    }
    fun scope(p: SchoolStudentProfile): CurriculumScope {
        val text = listOf(p.affiliation, p.faculty, p.department, p.admissionType).joinToString(" ")
        val f = faculty(text) ?: p.faculty.ifBlank { "大学院（所属待确认）" }
        val d = departments[f].orEmpty().filter { text.contains(it) }.maxByOrNull { it.length }.orEmpty()
        val level = detectLevel(text)
        val initial = CurriculumScope(f, d, p.curriculumYear, level = level)
        val matched = programs(initial).filter { (p.program + " " + text).contains(it) }
        val program = matched.singleOrNull() ?: p.program
        val track = tracks(initial).singleOrNull { text.contains(it) }.orEmpty()
        return initial.copy(program = program, track = track)
    }
    fun levels(faculty: String) = when(faculty) {
        SCIENCE -> listOf(StudyLevel.MASTER, StudyLevel.DOCTOR, StudyLevel.NON_DEGREE, StudyLevel.UNKNOWN)
        EDUCATION -> listOf(StudyLevel.PROFESSIONAL, StudyLevel.NON_DEGREE, StudyLevel.UNKNOWN)
        UNITED -> listOf(StudyLevel.DOCTOR, StudyLevel.NON_DEGREE, StudyLevel.UNKNOWN)
        else -> listOf(StudyLevel.MASTER, StudyLevel.NON_DEGREE, StudyLevel.UNKNOWN)
    }
    fun majors(f: String, level: StudyLevel) = departments[f].orEmpty().filter {
        f != SCIENCE || when(level) {
            StudyLevel.DOCTOR -> it in doctorMajors
            StudyLevel.MASTER -> it !in doctorMajors || it == "量子線科学専攻"
            else -> true
        }
    }
    fun programs(s: CurriculumScope): List<String> = when {
        s.faculty == EDUCATION -> educationCourses
        s.faculty == AGRICULTURE -> listOf("アジア展開農学コース", "実践農食科学コース", "応用植物科学コース", "地域共生コース")
        s.faculty == HUMAN && s.department == "人文科学専攻" -> listOf("文芸・思想コース", "歴史・考古学コース", "心理・人間科学コース", "公認心理師コース")
        s.faculty == HUMAN && s.department == "社会科学専攻" -> listOf("メディア・情報社会コース", "国際・地域共創コース", "法学・行政学コース", "経済学・経営学コース", "地域政策研究（社会人）コース")
        s.faculty == UNITED -> listOf("一般", "留学生特別プログラム")
        s.level != StudyLevel.MASTER -> emptyList()
        s.department == "理学専攻" -> listOf("数学・情報数理コース", "宇宙物理学コース", "化学コース ASM", "化学コース PSM", "生物学コース ASM", "生物学コース PSM", "地球環境科学コース")
        s.department == "情報工学専攻" -> listOf("情報システムプログラム", "情報科学プログラム", "情報マネジメントプログラム", "情報融合プログラム", "リカレントプログラム")
        s.department == "都市システム工学専攻" -> listOf("社会基盤デザインプログラム", "建築デザインプログラム", "社会人マスタープログラム")
        else -> emptyList()
    }
    fun tracks(s: CurriculumScope) = when(s.faculty) {
        HUMAN -> if(s.program == "公認心理師コース") listOf("一般専門教育", "リカレント専門教育") else listOf("一般専門教育", "リカレント専門教育", "留学生専門教育")
        EDUCATION -> listOf("学部新卒者等", "現職教員")
        else -> emptyList()
    }
    fun guide(s: CurriculumScope) = when(s.faculty) {
        HUMAN -> HUMAN_SOURCE
        EDUCATION -> EDUCATION_SOURCE
        AGRICULTURE -> "https://www.agr.ibaraki.ac.jp/graduate/relation/"
        UNITED -> UNITED_SOURCE
        SCIENCE -> if(s.level == StudyLevel.DOCTOR) "https://www.gse.ibaraki.ac.jp/collegelife/gs_doctor/subjects/" else "https://www.gse.ibaraki.ac.jp/collegelife/master/subjects/"
        else -> "https://www.ibaraki.ac.jp/"
    }
    private fun req(id: String, label: String, n: Number, children: List<CreditRequirement> = emptyList()) = CreditRequirement(id, label, n.toString().toBigDecimal(), children)
    fun rule(s: CurriculumScope): GraduateRule? {
        // Older or future rules must be reviewed independently; no silent year fallback.
        if(s.cohort != 2026 || s.level !in levels(s.faculty) || s.department !in majors(s.faculty,s.level) || s.level in setOf(StudyLevel.NON_DEGREE, StudyLevel.UNKNOWN)) return null
        if(programs(s).isNotEmpty() && s.program !in programs(s)) return null
        if(tracks(s).isNotEmpty() && s.track !in tracks(s)) return null
        val conditions = mutableListOf("在学期间、休学及长／短期履修认定：待学校确认", "指定必修课程、个别认定及类别配分：待核对")
        var notes = "仅依据学校已合格成绩统计；类别不明的学分保留待分类。修满学分不代表已取得学位。"
        var source = guide(s)
        var years = 2
        var total = 30
        val rows: List<CreditRequirement>
        when(s.faculty) {
            HUMAN -> {
                source = HUMAN_SOURCE
                val psych = s.program == "公認心理師コース"
                total = if(psych) 38 else 30
                val b = s.track == "リカレント専門教育"
                val c = s.track == "留学生専門教育"
                val group = mutableListOf(req("school", "研究科共通科目", 2), req("core", "コア専門科目", if(psych) 24 else if(b || c) 8 else 10), req("expanded", "拡充専門科目", 1))
                val career = req("career", "キャリア支援科目", if(psych || b) 0 else 2)
                if(c) group += career
                rows = listOf(req("university", "大学院共通科目", if(b) 0 else 2), req("research", "研究指導科目", 8),
                    req("combined", if(c) "研究科共通・キャリア・コア・拡充 合計" else "研究科共通・コア・拡充 合計", if(b) 22 else if(c) 20 else 18, group)) + if(c) emptyList() else listOf(career)
                conditions += listOf("テクノロジーと人間社会（必修1学分）：待核对", "硕士论文／获准的特定课题成果审查：待确认", "最终考试：待确认")
                notes += " 各类别显示下限，须同时满足组合学分及适用履修模型。日本語表現法不计入修了要求；CAP 额度不是修了最低学分。"
            }
            EDUCATION -> {
                source = EDUCATION_SOURCE; total = 48
                val first = s.program in educationCourses.take(3)
                rows = listOf(req("common", "共通科目", if(first) 20 else 18),req("specialty", "専門科目", if(first) 18 else 20),req("practicum", "実習科目", 10))
                conditions += listOf("课程内配分、教育实践及研究成果评价：待确认", "实习完成／学校批准的实习免除：待确认")
                notes += " 教职修士（専門職）不套用普通硕士论文条件；现职教师的实习免除或缩短学制须有正式批准，不自动减免学分。"
            }
            AGRICULTURE -> {
                source = AGRICULTURE_SOURCE
                val asia = s.program == "アジア展開農学コース"
                rows = listOf(req("university", "大学院共通科目", 2), req("school", "研究科共通科目", 3),
                    req("coreModule", "コアモジュール科目", 4)) +
                    (if(asia) listOf(req("other", "他の専門分野・専攻展開科目", 5)) else listOf(
                        req("ownOther", "自コース（他モジュール）科目", 3),req("other", "他コース科目または専攻展開科目", 2))) +
                    req("courseCommon", "コース共通専攻科目", 14)
                conditions += listOf("指定专业领域配分（自专业领域4学分等）：待核对", "硕士论文审查：待确认", "最终考试：待确认")
            }
            UNITED -> {
                source = UNITED_SOURCE; total = 12; years = 3
                rows = listOf(req("required", "必修科目", 9.5),req("elective", "選択科目", 2.5))
                conditions += listOf("博士论文审查及最终考试：待确认", "必要研究指导、成果发表及申请资格：待确认")
                if(s.program == "留学生特別プログラム") conditions += "留学生特别项目指定选修（实习、全球特论等）：待核对"
                notes += " 学位授予单位为东京农工大学。本应用不会连接其他大学教务系统；课程数据仍仅来自已有的茨城大学同步。"
            }
            SCIENCE -> {
                if(s.level == StudyLevel.DOCTOR) {
                    source = DOCTOR_SOURCE; total = 14; years = 3
                    rows = listOf(req("school", "研究科共通科目", 2),req("required", "専攻必修科目", 8),req("elective", "専攻選択科目", 4))
                    conditions += listOf("主指导教师及其他教师课程配分：待核对", "博士论文审查：待确认", "最终考试及论文申请资格：待确认")
                } else if(s.department == "理学専攻") {
                    source = SCIENCE_SOURCE
                    val n = when(s.program) { "生物学コース ASM" -> 8; "生物学コース PSM" -> 7; "地球環境科学コース" -> 12; else -> 4 }
                    rows = listOf(req("university", "大学院共通科目", 2),req("school", "研究科共通科目", 3),req("required", "コース必修科目", n),req("selectRequired", "コース選択必修科目", 16-n),req("elective", "選択科目", 9))
                    conditions += listOf(if(s.program.endsWith("PSM")) "研究成果报告、作品集及最终考试：待确认" else "硕士论文审查及最终考试：待确认", "研究伦理教育与研究指导：待确认")
                } else {
                    source = MASTER_SOURCE
                    val standard = listOf(req("university", "大学院共通科目", 2),req("school", "研究科共通科目", 3))
                    fun major(required: Int, core: Int, select: Int, elective: Int) = listOf(req("required", "専攻必修", required),req("core", "プログラムコア科目", core),req("selectRequired", "選択必修", select),req("elective", "選択科目", elective))
                    rows = when(s.department) {
                        "量子線科学専攻" -> standard + major(8,4,1,2) + req("specialty", "専門科目", 10)
                        "機械システム工学専攻" -> standard + major(11,4,8,2)
                        "電気電子システム工学専攻" -> standard + major(12,4,7,2)
                        "情報工学専攻" -> standard + if(s.program == "リカレントプログラム") major(12,5,6,2) else major(6,5,6,8)
                        "都市システム工学専攻" -> if(s.program == "社会人マスタープログラム") listOf(req("required", "専攻必修", 8),req("combined", "共通科目を含む指定科目", 22)) else standard + major(8,6,6,5)
                        else -> return null
                    }
                    conditions += listOf("所属专业方向指定必修、选修及跨专业认定：待核对", "研究伦理教育与研究指导：待确认", "硕士论文审查：待确认", "最终考试：待确认")
                }
            }
            else -> return null
        }
        return GraduateRule(total, rows, source, conditions, notes, years)
    }
    fun missing(s: CurriculumScope): String = when {
        s.level == StudyLevel.NON_DEGREE -> "非学位身份：保留课程与成绩，不显示修了学分目标或学位完成率。"
        s.level in setOf(StudyLevel.UNKNOWN, StudyLevel.UNDERGRADUATE) -> "请确认大学院学位层次，不根据学号猜测。"
        s.cohort != 2026 -> "该规则年度尚未核对；可保留学籍与成绩，不套用其他年度规则。"
        s.department !in majors(s.faculty,s.level) -> "请确认研究科与专攻。"
        programs(s).isNotEmpty() && s.program !in programs(s) -> "请选择适用专业方向。"
        tracks(s).isNotEmpty() && s.track !in tracks(s) -> "请选择适用履修类型。"
        else -> "该组合的修了要求尚待核对。"
    }
    fun leaves(rule: GraduateRule): List<CreditRequirement> {
        fun walk(n: CreditRequirement): List<CreditRequirement> = if(n.children.isEmpty()) listOf(n) else n.children.flatMap(::walk)
        return rule.requirements.flatMap(::walk)
    }
    fun classify(s: CurriculumScope, g: PortalGrade): CourseClassification {
        val rule = rule(s) ?: return CourseClassification(null, "待分类")
        val norm = UniversityCourseClassification::normalize
        val path = g.category.split('/').map(norm)
        if(path.any { it in setOf("修了要件外", "修了要件外科目", "修了要件に算入しない科目", "自由科目", "卒業要件外科目") } || (s.faculty == HUMAN && norm(g.name) == "日本語表現法"))
            return CourseClassification(null, "修了要求外", excluded = true)
        val all = leaves(rule)
        val aliases = if(s.faculty == AGRICULTURE) mapOf("コース共通科目" to "courseCommon", "自コース(他モジュール)科目" to "ownOther",
            "他コース科目又は専攻展開科目" to "other") else emptyMap()
        val matches = all.filter { norm(it.label) in path || path.any { p -> aliases[p] == it.id } }
        // Ambiguous hierarchy is not assigned to whichever leaf happens to be first.
        val exact = matches.singleOrNull() ?: return CourseClassification(null, "待分类")
        return CourseClassification(exact.id, exact.label, "学校成绩分类")
    }
    fun progress(s: CurriculumScope, grades: List<PortalGrade>): GraduationProgress {
        val rule = rule(s) ?: return GraduationProgress(emptyList(), BigDecimal.ZERO)
        return GraduationProgressCalculator.calculate(listOf(req("total", "修了学分", rule.total, rule.requirements)),
            grades.mapIndexedNotNull { i,g -> if(!g.passed) null else classify(s,g).let { if(it.excluded) null else AwardedCredit("grade-$i",g.credits,it.categoryId) } })
    }
}

package com.tyust.course.academic

/** A curriculum cohort is independent of the student's current year of study. */
data class CurriculumScope(val faculty: String, val department: String, val cohort: Int?, val program: String = "",
    val level: StudyLevel = StudyLevel.UNDERGRADUATE, val track: String = "")
data class RequirementSummary(val rows: List<Pair<String, Int>>, val source: String, val page: Int,
    val detail: String = "", val detailedInformation2026: Boolean = false, val total: Int = 124)

object UniversityCurricula {
    val departments = linkedMapOf(
        "人文社会科学部" to listOf("現代社会学科", "法律経済学科", "人間文化学科"),
        "教育学部" to listOf("学校教育教員養成課程", "養護教諭養成課程"),
        "理学部" to listOf("理学科"),
        "工学部" to listOf("機械システム工学科", "電気電子システム工学科", "物質科学工学科", "情報工学科", "都市システム工学科"),
        "農学部" to listOf("食生命科学科", "地域総合農学科"),
        "地域未来共創学環" to listOf("地域未来共創学環")
    )
    fun scope(p: SchoolStudentProfile): CurriculumScope {
        if (GraduateCurricula.detectLevel(p.affiliation + " " + p.faculty + " " + p.admissionType) != StudyLevel.UNDERGRADUATE)
            return GraduateCurricula.scope(p)
        val department = departments[p.faculty]?.filter { p.department.startsWith(it) }?.maxByOrNull { it.length }
            ?: if (p.faculty == "地域未来共創学環") p.faculty else p.department
        return CurriculumScope(p.faculty, department, p.curriculumYear,
            p.program.ifBlank { p.department.removePrefix(department).trim() })
    }
    fun owner(p: SchoolStudentProfile) = java.security.MessageDigest.getInstance("SHA-256")
        .digest(p.studentNumber.toByteArray()).joinToString("") { "%02x".format(it) }
    fun profileMatches(p: SchoolStudentProfile?, snapshotKey: String) = p != null && snapshotKey.substringBefore('/') == owner(p)
    fun programs(s: CurriculumScope): List<String> = when (s.department) {
        "都市システム工学科" -> listOf("社会基盤デザインプログラム", "建築デザインプログラム")
        "機械システム工学科" -> listOf("環境エネルギーシステムプログラム", "生産システムプログラム", "制御システムプログラム")
        "電気電子システム工学科" -> listOf("エネルギーシステムプログラム", "エレクトロニクスシステムプログラム")
        "物質科学工学科" -> listOf("材料工学プログラム", "化学・生命工学プログラム")
        "理学科" -> listOf("数学・情報数理コース 数学プログラム", "数学・情報数理コース 情報数理プログラム", "物理学コース", "化学コース", "生物科学コース", "地球環境科学コース", "地球環境科学コース 地球科学技術者養成プログラム", if (s.cohort == 2024) "学際理学コース" else "総合理学コース")
        "現代社会学科" -> listOf("メディア文化メジャー", "国際・地域共創メジャー")
        "法律経済学科" -> listOf("法学・行政学メジャー", "経済学・経営学メジャー")
        "人間文化学科" -> listOf("文芸・思想メジャー", "歴史・考古学メジャー", "心理・人間科学メジャー")
        "学校教育教員養成課程" -> listOf("教育実践科学コース", "特別支援教育コース") + listOf("国語", "社会", "英語", "数学", "理科", "音楽", "美術", "保健体育", "技術", "家庭").flatMap { subject -> listOf("教科教育コース ${subject}選修 Aタイプ", "教科教育コース ${subject}選修 Bタイプ") }
        "食生命科学科" -> if (s.cohort != null && s.cohort <= 2024) listOf("国際食産業科学コース", "バイオサイエンスコース") else emptyList()
        "地域総合農学科" -> if(s.cohort == 2024) listOf("農業科学コース", "地域共生コース 環境保全学系", "地域共生コース 工学系", "地域共生コース 社会科学系")
            else listOf("応用植物科学コース", "地域共生コース", "地域共生コース 特別カリキュラム（測量士補）")
        "地域未来共創学環" -> listOf("地域ビジネスデザインプログラム", "地域創生データサイエンスプログラム")
        else -> emptyList()
    }
    fun guide(s: CurriculumScope): String = when(s.faculty) {
        "工学部" -> "https://www.eng.ibaraki.ac.jp/education/class/"
        "理学部" -> "https://www.sci.ibaraki.ac.jp/collegelife/curriculum/"
        "人文社会科学部" -> "https://www.hum.ibaraki.ac.jp/reference/for-enrolled.html"
        "教育学部" -> "https://www.edu.ibaraki.ac.jp/students/zaigaku/"
        "農学部" -> "https://www.agr.ibaraki.ac.jp/teaching/course/"
        "地域未来共創学環" -> "https://www.mirai.ibaraki.ac.jp/students/"
        else -> "https://www.ibaraki.ac.jp/"
    }
    private val engineeringFiles = mapOf(2024 to "2024-course-registration10.pdf", 2025 to "2025-course-registration06.pdf", 2026 to "2026-course-registration03.pdf")
    /** Only reviewed source/cohort pairs can produce numeric requirements. Never fall back to another faculty. */
    fun summary(s: CurriculumScope): RequirementSummary? {
        if (GraduateCurricula.isGraduate(s)) return null
        if (s.cohort !in 2024..2026 || s.department !in departments[s.faculty].orEmpty()) return null
        if (s.faculty == "工学部") {
            val required = when (s.department) {
                "機械システム工学科" -> 71
                "電気電子システム工学科" -> 71
                "物質科学工学科" -> 71
                "情報工学科" -> 71
                "都市システム工学科" -> when(s.program) {
                    "社会基盤デザインプログラム" -> 54
                    "建築デザインプログラム" -> 69
                    else -> return null
                }
                else -> return null
            }
            return RequirementSummary(listOf("基盤教育科目" to 24, "専門必修" to required,
                "専門選択必修" to 92-required, "自由履修" to 8),
                "https://www.eng.ibaraki.ac.jp/common/education/class/${engineeringFiles.getValue(s.cohort!!)}", 6,
                detailedInformation2026 = s.cohort == 2026 && s.department == "情報工学科" && s.program.isBlank())
        }
        if(s.faculty == "人文社会科学部") {
            val human = s.department == "人間文化学科"
            return RequirementSummary(listOf("基盤教育科目" to 22, "学部基礎科目" to if(human) 8 else 6,
                "学科基礎ゼミナール" to 1, "学科専門科目" to 50, "メジャー基礎ゼミナール" to 2,
                "メジャー専門ゼミナール" to 8, "卒業研究" to 8, "自由履修" to if(human) 25 else 27),
                "https://www.hum.ibaraki.ac.jp/pdf/${s.cohort.toString().takeLast(2)}requirements.pdf",
                if(human) 25 else if(s.department == "法律経済学科") 21 else 17,
                "须完成主修、副修及指定层级课程；不能仅按学分总数判断毕业。")
        }
        if(s.faculty == "理学部") {
            val values = when(s.program) {
                "数学・情報数理コース", "数学・情報数理コース 数学プログラム", "数学・情報数理コース 情報数理プログラム" -> listOf(20,20,20,26)
                "物理学コース" -> listOf(23,26,20,17)
                "化学コース" -> listOf(19,29,33,5)
                "地球環境科学コース" -> listOf(21,23,30,12)
                "地球環境科学コース 地球科学技術者養成プログラム" -> listOf(22,23,41,9)
                else -> null
            }
            val free = if(s.program.endsWith("地球科学技術者養成プログラム")) 5 else 14
            val rows = if(values != null) listOf("専門基礎科目", "専門標準科目", "専門発展科目", "専門科目選択履修").zip(values)
                else if(s.program == "生物科学コース") listOf("専門基礎科目" to 16, "専門標準・発展科目" to 46, "専門科目選択履修" to 24)
                else if(s.program in listOf("総合理学コース", "学際理学コース")) listOf("専門科目" to 86)
                else return null
            return RequirementSummary(listOf("基盤教育科目" to 24) + rows + ("自由履修" to free),
                "https://www.sci.ibaraki.ac.jp/tpl/wp-content/uploads/${s.cohort}${if(s.cohort == 2024) "risyuyoko" else "rishuyoko"}.pdf", 37,
                "按所属教育项目核对具体必修、选修和毕业研究条件。")
        }
        if(s.faculty == "教育学部") {
            val professional = when {
                s.department == "養護教諭養成課程" -> 81
                s.program == "教育実践科学コース" -> 88
                s.program == "特別支援教育コース" -> 92
                s.program in programs(s) && s.program.endsWith(" Aタイプ") -> 79
                s.program in programs(s) && s.program.endsWith(" Bタイプ") -> 83
                else -> return null
            }
            return RequirementSummary(listOf("基盤教育科目" to 22, "専門科目" to professional, "自由履修" to 102-professional),
                "https://www.edu.ibaraki.ac.jp/students/zaigaku/${s.cohort}rishuyoukou.pdf", if(professional==81) 18 else 13,
                "还须满足对应教师资格和实习条件；A／B 类型的要求不同。")
        }
        if(s.faculty == "農学部") {
            val special = s.cohort != 2024 && s.program.startsWith("地域共生コース")
            if(s.department == "地域総合農学科" && s.program !in programs(s)) return null
            if(s.cohort == 2024 && s.department == "食生命科学科" && s.program !in programs(s)) return null
            return RequirementSummary(listOf("基盤教育科目" to 24, "専門科目" to if(special) 90 else 86, "自由履修（最低）" to 0),
                "https://www.agr.ibaraki.ac.jp/assets/images/summary/pdf/course/schoolofagr_guidance_for_${s.cohort}freshman${if(s.cohort == 2024) "" else "2"}.pdf", 16,
                "各类别最低学分之外，还须补足毕业总学分；专业必修和选修按所属课程核对。")
        }
        if(s.faculty == "地域未来共創学環") {
            val id = when(s.cohort) { 2026 -> "1CO6aHJvUldpKqr9gpsafTLpzTXpcvm8V"; 2025 -> "1jj8cSA4-8ehEA3-qgLVMc2wuB1lC-e1C"; else -> "15u1Pm9xljpQwOmSvUT4beyJ0F_otm7do" }
            return RequirementSummary(listOf("基盤教育科目" to 26, "専門科目" to 88, "自由履修" to 10),
                "https://drive.google.com/file/d/$id/view", 0,
                "还须满足专业方向、コーオプ实习及毕业研究条件。")
        }
        return null
    }
}


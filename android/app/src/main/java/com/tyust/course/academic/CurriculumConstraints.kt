package com.tyust.course.academic

/** Reviewed common-education restrictions, not a complete professional/major graduation audit. */
object CurriculumConstraints {
    fun source(year: Int): String { return "https://drive.google.com/file/d/" + when (year) {
        2024 -> "1Ddr855lCNPrbGKrKBf6-9df9cCBDrVRA"
        2025 -> "1ij3Y-7w_7L9hOA64TeMxksg_jEtjd_G3"
        2026 -> "1U-r7ebg_4NjCMWCn3sFjuEf5htQ8q_-_"
        else -> return "https://sites.google.com/g.ibaraki.ac.jp/ssc/guidebooks"
    } + "/view" }

    fun rules(s: CurriculumScope): List<EnrollmentRule> {
        if (s.cohort !in 2024..2026 || s.department !in UniversityCurricula.departments[s.faculty].orEmpty()) return emptyList()
        val human = s.faculty == "人文社会科学部"
        val education = s.faculty == "教育学部"
        val future = s.faculty == "地域未来共創学環"
        val pages = when (s.faculty) {
            "人文社会科学部" -> "42–44"
            "教育学部" -> "46–48"
            "理学部" -> "49–51"
            "工学部" -> "52–54"
            "農学部" -> "55–57"
            else -> "58–60"
        }
        val url = source(s.cohort!!)
        fun note(id: String, title: String, text: String) = EnrollmentRule(id, title,
            "${s.cohort} 入学 · ${s.faculty} · ${s.department}", text, pages, source = url)
        val culture = if (human) 2 else 3
        val natural = if (future) 6 else 4
        val elective = if (future) 4 else if (education) 0 else 3
        val result = EnrollmentRules.all.filter { it.id !in setOf("overflow", "physical", "ethics", "life") }
            .map { it.copy(source = url, page = if (it.page.contains("53")) pages else it.page) }.toMutableList()
        result += note("minimums", "毕业最低学分与上限分开", "本页 x/y 的 y 是毕业最低要求，不是最多可修学分。多文化理解合计 $culture 学分，自然と社会の広がり合计 $natural 学分。未单列最低值的子类只显示已修学分；仍须遵守组合、先修和选课上限。")
        result += note("culture_combination", "多文化理解的组合", if (future)
            "多文化コミュニケーション 1 学分、ヒューマニティーズ 2 学分，不能用其中一类的超额学分代替另一类。初修外国语、共生、P&A 是前一类的选择项，不是每项都要修 1 学分。"
        else "毕业最低 $culture 学分由多文化コミュニケーション与ヒューマニティーズ组合，原文允许组合不限。履修安排：一年级前期修ヒューマニティーズ${if (human) "（也可 Q1、Q2 各修 1 学分）" else " 2 学分"}，后期从初修外国语、共生、P&A 中选 1 门。不能把安排中的数字当成每个子类独立毕业最低值。")
        result += note("nature_combination", "自然与社会的组合", if (future)
            "自然・環境と人間 2 学分、グローバル化と人間社会 4 学分。其中一年级后期アントレプレナーシップ入門Ⅰ、Ⅱ各 1 学分，二年级后期再修 2 学分。"
        else "两类合计 4 学分；通常组合不限。履修安排为前期自然类 2 学分、后期社会类 2 学分；下列指定课程或项目例外仍须满足。")
        if (education) result += note("constitution_required", "日本国憲法必修", "グローバル化と人間社会中必须修日本国憲法 2 学分。原则上同一学期、同一教师连续履修，其他社会类课程不能代替此必修。")
        if (s.faculty == "工学部" && s.department == "機械システム工学科" && s.cohort == 2026)
            result += note("environment_required", "環境と人間必修", "自然・環境と人間中必须修環境と人間 1 学分；不是其他自然类课程均可替代。")
        if (s.faculty == "理学部" && s.cohort == 2026)
            result += note("science_combination", "理学部指定项目例外", "地球環境科学コース的地球科学技術者養成プログラム、総合理学コース的 E-J プログラム：自然・環境と人間与グローバル化と人間社会各 2 学分。其他项目不能直接套用此指定组合；请按自己的项目核对。")
        if (human) result += note("foreign_language", "初修外国语与第二外国语", "基盘教育初修外国语不等于专业科目第二外国语。现代社会、法律经济学科选择基盘初修外国语时，必须不同于专业已修第二外国语；人间文化学科一年级不能修基盘初修外国语，二年级起也须选择不同语言。专业第二外国语的必修与学分须按学科专业授业计划核对，不能把传闻的 2 学分写入所有人的基盘分类。")
        result += note("overflow", "超额学分的去向", if (education)
            "多文化理解超过 3 学分、自然与社会超过 4 学分的部分计入自由履修；不能据此忽略必修日本国憲法。"
        else "多文化理解超过 $culture 学分、自然与社会超过 $natural 学分的部分计入选择履修；选择履修最低 $elective 学分，超过部分计入自由履修。App 保留学校原始归类；学校尚未确认的转入不自动视为已完成。")
        if (s.cohort == 2024 && s.faculty in setOf("理学部", "工学部")) {
            result += note("physical", "身体活動及 2024 修正", "身体活動最低 1 学分，超额可计选择或自由履修。2024 年学校修正表替换了本项原文，不能套用其他年度的每年 1 门限制；实际班级与追加资格仍以学校安排为准。")
                .copy(source = "https://drive.google.com/file/d/1e6qHOUlbj7_V3q94qwD9nyOnJO6C9mrs/view", page = "1")
        } else result += EnrollmentRules.all.single { it.id == "physical" }.copy(source = url, page = pages,
            detail = "身体活動原则上每学年 1 门；${if (education) "须按教育学部教师资格要求修身体活動合计 2 学分。" else "必需 1 学分，超额按所属学部选择或自由履修规则处理。"}须参加指定指导和分班；出席至少达到总课时 3/4。")
        if (!human) result += EnrollmentRules.all.single { it.id == "ethics" }.copy(source = url, page = pages, detail = "科学と倫理必修 1 学分，不得超额履修；按本学部指定时间和班级。")
        if (!future) result += EnrollmentRules.all.single { it.id == "life" }.copy(source = url, page = pages, detail = "ライフデザイン必修 1 学分，不得超额履修；三年级按所属学部指定学期。")
        result += note("annual_cap", "年度 CAP 与审批例外", "原则上年度登记上限 46 学分，毕业要求外和集中讲义的除外范围按所属学部核对。教师资格、成绩优秀等放宽必须经过学校审批；不自动获得更高额度。地域未来共创学环专业集中讲义另有计入规则，不能全部排除。")
            .copy(limit = 46.toBigDecimal(), scope = "同一学年度 · 按学校 CAP 计入范围", source = UniversityCurricula.guide(s), page = "所属学部履修要项 · CAP")
        return result
    }
}

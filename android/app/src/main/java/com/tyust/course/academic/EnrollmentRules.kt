package com.tyust.course.academic

import java.math.BigDecimal

data class EnrollmentRule(val id: String, val title: String, val scope: String, val detail: String,
    val page: String, val limit: BigDecimal? = null, val unit: String = "学分")

/** Rules scoped to 2026-entry engineering students, not other faculties' requirements. */
object EnrollmentRules {
    const val source = "https://drive.google.com/file/d/1U-r7ebg_4NjCMWCn3sFjuEf5htQ8q_-_/view"
    private fun cap(id: String, title: String, scope: String, limit: String, detail: String, page: String, unit: String = "学分") =
        EnrollmentRule(id, title, scope, detail, page, limit.toBigDecimal(), unit)
    val all = listOf(
        cap("multicultural", "多文化コミュニケーション", "同一学期（前期 Q1+Q2，后期 Q3+Q4）", "1", "初修外语、共生とコミュニケーション、パフォーマンス＆アート合并核对。同一学期不能修 2 学分；不要在 Q3、Q4 各选 1 学分。此限制不适用于整个多文化理解或ヒューマニティーズ。", "23、53–54"),
        cap("constitution", "日本国憲法", "同一学期", "2", "原则上同一教师的 1+1 学分课程须在同一学期连续履修。重修也遵守；原教师原题目再次开课时，可只重修未通过部分。", "24"),
        cap("physical", "身体活動", "同一学年", "1", "原则上每年只修 1 门，从一年级开始；不及格通常到下一学年重修。须参加四月指导及指定班级分配，出席至少达到总课时的 3/4。", "22、40、53", "门"),
        cap("seminar", "大学入門ゼミ", "该科目累计履修", "2", "一年级前期，按指定班级履修。", "20、53"),
        cap("ibaraki", "茨城学", "该科目累计履修", "1", "一年级 Q2，按指定班级履修。", "20、53"),
        cap("english", "プラクティカル・イングリッシュ", "该科目累计履修", "4", "一年级 IE 共 4 学分，不得重复履修同一科目或自行更改分班等级。重修按学校安排核对。", "21、53"),
        cap("literacy", "情報リテラシー", "该科目累计履修", "2", "一年级前期，按指定班级与时间履修。", "21、53"),
        cap("ai", "データサイエンス・AI入門", "该科目累计履修", "2", "一年级后期，按指定班级与时间履修。", "22、53"),
        cap("ethics", "科学と倫理", "该科目累计履修", "1", "工学部一年级 Q1，按指定班级履修。", "23、53"),
        cap("life", "ライフデザイン", "该科目累计履修", "1", "工学部三年级 Q1 或 Q2，按指定班级履修。", "23、53"),
        EnrollmentRule("english_prerequisite", "英语 C/D 的先修条件", "选修资格", "前期 Integrated English A 和 B 必须均通过，才能履修后期 C 和 D；沿用前期的 1/2/3 分班等级。", "21"),
        EnrollmentRule("duplicate", "重复修读与学分认定", "重复核对", "通识课已通过的同一教师、同一授课题目不能再次取得学分；同一教师但题目不同可作为另一门课。初修外语同一语言、同一课程名称不能重复取得学分。不能仅凭大类名相同判重复。", "23–24"),
        EnrollmentRule("japanese", "学術日本語的资格与分级", "留学生课程", "面向外国留学生；通过私费外国人留学生选拔入学的非留学在留资格外国籍学生也适用。分班未达要求须修Ⅰ，达到要求不能修Ⅰ；学校分班结果尚未导入。", "23–24"),
        EnrollmentRule("lottery", "事前申告、抽签与班级", "选课流程", "主体学修需先申告并按抽签结果履修。受讲资格不能转让；满额班即使出现退出者也不追加许可。未中签或想追加课程，应查空位并联系学习支持室。选课须遵守指定学部、年级和班级。", "30–32、53"),
        EnrollmentRule("attendance", "出席要求", "取得学分条件", "通常至少出席总课时的 2/3，身体活動至少 3/4；课程还可能有更严格条件。缺课特殊处理须按学校程序办理，App 不将满足最低出席比例等同于必然及格。", "24"),
        EnrollmentRule("overflow", "类别超额学分的去向", "毕业分类", "工学部多文化理解超过 3 学分、自然与社会超过 4 学分的部分计入选择履修；选择履修超过 3 学分的部分转自由履修。身体活动超过必需 1 学分的部分可计选择或自由履修，不应当作全部无效。", "53–54"),
        cap("recognized", "入学前及校外学习认定", "所述认定制度累计", "60", "须申请并经学校认定；入学前在本校取得的学分不含在此 60 学分中。此额度不同于远程授课 60 学分上限，二者不能相加视为毕业额度。", "24–25")
    )
    fun remaining(rule: EnrollmentRule, existing: BigDecimal, planned: BigDecimal): BigDecimal {
        require(rule.limit != null && existing.signum() >= 0 && planned.signum() >= 0)
        if (rule.unit == "门") require(existing.stripTrailingZeros().scale() <= 0 && planned.stripTrailingZeros().scale() <= 0)
        return rule.limit - existing - planned
    }
    fun englishStatus(grades: List<PortalGrade>): String {
        val passed = grades.filter { it.passed }.map { it.name.trim() }
        val levels = (1..3).filter { level -> listOf("A", "B").all { suffix -> passed.any { it == "Integrated English $level$suffix" } } }
        return if (levels.size == 1) "已保存成绩中 IE ${levels.single()}A、${levels.single()}B 均通过；满足文件所述 C/D 的成绩先修条件，仍须遵守学校分班。"
        else "现有记录无法确认同一分班等级的 A、B 均通过，请核对完整成绩及学校分班。"
    }
}

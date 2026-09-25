package com.tyust.course.academic

import java.math.BigDecimal
import java.text.Normalizer

/** Header-driven parsing: column count/order varies between graduate and undergraduate tables. */
object SchoolGradeTable {
    private fun normalized(s: String) = Normalizer.normalize(s, Normalizer.Form.NFKC).replace(Regex("\\s+"), "")
    private val names = setOf("科目", "科目名", "授業科目名")
    private val credits = setOf("単位数", "単位")
    fun isHeader(line: String): Boolean {
        val cells=line.split(" | ").map(::normalized)
        return cells.any { it in names } && cells.any { it in credits } && "合否" in cells
    }
    fun parse(text: String): PortalImport? {
        val lines=text.lines().map(String::trim)
        val headers=lines.indices.filter { isHeader(lines[it]) }
        if(headers.isEmpty()) return null
        require(headers.size == 1) { "存在多份成绩表，无法确认读取范围" }
        val start=headers.single()
        val header=lines[start].split(" | ").map(::normalized)
        require(header.distinct().size == header.size) { "成绩列名重复，未保存" }
        fun column(labels: Set<String>, required: Boolean=true): Int {
            val found=header.indices.filter { header[it] in labels }
            require(found.size <= 1 && (!required || found.size == 1)) { "成绩表缺少或重复关键列，未保存" }
            return found.singleOrNull() ?: -1
        }
        val name=column(names);val credit=column(credits)
        val year=column(setOf("修得年度", "年度"));val term=column(setOf("修得学期", "学期"))
        val score=column(setOf("評点", "点数"),false);val evaluation=column(setOf("評語", "評価"));val passed=column(setOf("合否"))
        val number=column(setOf("No.", "No", "番号"),false)
        val categories=header.indices.filter { header[it] in setOf("科目大区分", "科目中区分", "科目小区分", "科目区分", "大区分", "中区分", "小区分") }
        val serials=mutableSetOf<Int>()
        val tableLines=lines.drop(start+1).takeWhile { it.isNotBlank() }
        val remaining=lines.drop(start+1+tableLines.size)
        require(remaining.none { line ->
            val c=line.split(" | ")
            c.size == header.size && ((number>=0 && c[number].toIntOrNull()!=null) ||
                (number<0 && c[credit].toBigDecimalOrNull()!=null && c[year].matches(Regex("[0-9]{4}"))))
        }) { "成绩表存在分段数据，无法确认读取范围" }
        val grades=tableLines.map { line ->
            val c=line.split(" | ").map(String::trim)
            require(c.size == header.size) { "成绩行与表头列数不一致，未保存" }
            if(number>=0) require(c[number].toIntOrNull()?.let { it > 0 && serials.add(it) } == true) { "成绩行号无效或重复，未保存" }
            val n=normalized(c[credit]).toBigDecimalOrNull()
            require(n != null && n > BigDecimal.ZERO && n <= 30.toBigDecimal()) { "学分无法识别" }
            val pass=normalized(c[passed])
            require(pass in setOf("合", "否", "合格", "不合格")) { "包含未确定成绩，未保存" }
            val ok=pass in setOf("合", "合格")
            val rawScore=if(score<0) "" else normalized(c[score])
            val numericScore=rawScore.toIntOrNull()
            require(numericScore != null || rawScore in setOf("", "-", "―", "—", "認定", "認", "合", "否", "合格", "不合格", "*", "＊")) { "成绩分数无法识别，未保存" }
            require(numericScore == null || numericScore in 0..100 && (numericScore>=60) == ok) { "分数与合否不一致，未保存" }
            require(c[name].isNotBlank() && c[evaluation].isNotBlank()) { "课程名称或评语为空，未保存" }
            PortalGrade(c[name],n,numericScore,c[evaluation],ok,c[year],c[term],categories.map { c[it] }.filter(String::isNotBlank).joinToString(" / "))
        }
        require(grades.isNotEmpty()) { "成绩为空，保留原记录" }
        val earned=grades.filter { it.passed }.fold(BigDecimal.ZERO) { a,g -> a+g.credits }
        val totals=Regex("修得単位数\\s*\\|\\s*([0-9.]+)").findAll(Normalizer.normalize(text, Normalizer.Form.NFKC)).map { it.groupValues[1].toBigDecimal() }.toList()
        require(totals.isNotEmpty() && totals.all { it.compareTo(earned)==0 }) { "明细学分与学校总数不一致，请显示全部成绩后读取" }
        val gpaStart=lines.indexOfFirst { it == "年度・学期 | 学期GPA | 年間GPA | 通算GPA" }
        val gpa=if(gpaStart>=0) lines.getOrNull(gpaStart+1)?.split(" | ")?.takeIf { it.size==4 }?.last() else null
        return PortalImport("grades", "学校成绩 · ${grades.size} 门 · 已修 $earned 学分", listOf("学校通算 GPA：${gpa ?: "未提供"}（直接采用学校显示值）") + grades.map {
            "${it.name}\n${it.credits} 学分 · ${it.score?.toString() ?: "无百分制分数"} · ${it.grade} · ${if(it.passed) "通过" else "未通过"}\n${it.year} · ${it.term}\n${it.category}"
        },grades=grades)
    }
}

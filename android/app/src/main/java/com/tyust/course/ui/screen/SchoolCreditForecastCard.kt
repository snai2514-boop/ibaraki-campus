package com.tyust.course.ui.screen

import com.tyust.course.i18n.LocalizedText as Text
import androidx.compose.material3.Text as RawText


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.*
import java.util.Calendar

@Composable
fun SchoolCreditForecastCard(grade: PortalImport, tables: List<PortalImport>, faculty: String? = null) {
    val today = Calendar.getInstance(IbarakiTimetable.zone)
    val month = today.get(Calendar.MONTH) + 1
    val calendarYear = today.get(Calendar.YEAR)
    var year by remember(grade.key) { mutableIntStateOf(if (month < 4) calendarYear - 1 else calendarYear) }
    var period by remember(grade.key) { mutableIntStateOf(if (month in 4..9) 4 else 5) }
    var expanded by remember { mutableStateOf(false) }
    val choices = listOf("Q1", "Q2", "Q3", "Q4", "前期 Q1+Q2", "后期 Q3+Q4")
    val quarters = when(period) { 4 -> setOf(1,2); 5 -> setOf(3,4); else -> setOf(period + 1) }
    val forecast = remember(grade, tables, year, period, faculty) { SchoolCreditForecast.calculate(grade, tables, year, quarters, faculty) }
    fun n(v: java.math.BigDecimal) = v.stripTrailingZeros().toPlainString()
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("学分预览", style = MaterialTheme.typography.titleLarge)
        Row {
            TextButton(onClick = { year-- }) { Text("‹") }
            Text("$year 学年度", Modifier.padding(top = 12.dp))
            TextButton(onClick = { year++ }) { Text("›") }
        }
        Column {
            choices.takeLast(2).forEachIndexed { i, label -> FilterChip(period == i + 4, { period = i + 4 }, label = { Text(label) }) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                choices.take(4).forEachIndexed { i, label -> FilterChip(period == i, { period = i }, label = { Text(label) }) }
            }
        }
        Text("跨学季课程仅计一次。", style = MaterialTheme.typography.bodySmall)
        if (forecast.missing.isNotEmpty()) Text("尚缺 ${forecast.missing.joinToString { "Q$it" }} 课表，仅预览已有课程。", color = MaterialTheme.colorScheme.error)
        if (forecast.courses.isEmpty()) {
            Text("暂无课程，等待学校发布。", style = MaterialTheme.typography.titleMedium)
            Text("当前已修 ${n(forecast.earned)} 学分，预计总分待定。")
        } else {
        Text("本期可获得：${n(forecast.offered)} 学分")
        Text("已修 ${n(forecast.completed)} 学分 · 不合格 ${n(forecast.failed)} 学分")
        Text("待出成绩课程全部合格：新增 ${n(forecast.added)} 学分。")
        Text("预计总分：${n(forecast.earned)} + ${n(forecast.added)} = ${n(forecast.total)} 学分", style = MaterialTheme.typography.titleMedium)
        Text("仅为预估，以学校认定为准。", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起明细" else "课程明细") }
        if (expanded) Text("已修得不重复增加；不合格课程不算新增。跨学季课程在结束学季计分。", style = MaterialTheme.typography.bodySmall)
        if (expanded) forecast.courses.forEach { c -> Text("${c.code} ${c.name} · ${n(c.credits)} 学分\n${c.quarter?.let { "Q$it · " }.orEmpty()}${c.state}", style = MaterialTheme.typography.bodySmall) }
        }
    } }
}

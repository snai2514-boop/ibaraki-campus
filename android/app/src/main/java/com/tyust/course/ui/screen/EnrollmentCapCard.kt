package com.tyust.course.ui.screen

import com.tyust.course.i18n.LocalizedText as Text
import androidx.compose.material3.Text as RawText


import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.EnrollmentCap
import com.tyust.course.academic.IbarakiCurriculum

@Composable
fun EnrollmentCapCard() {
    val context = LocalContext.current
    var registered by rememberSaveable { mutableStateOf("") }
    var planned by rememberSaveable { mutableStateOf("") }
    var available by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    fun number(value: String) = value.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 && it <= 200.toBigDecimal() }
    val base = number(registered); val add = number(planned); val period = number(available)
    val result = if (base != null && add != null && (available.isBlank() || period != null)) EnrollmentCap.assess(base, add, period) else null
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("选课上限", style = MaterialTheme.typography.titleLarge)
            Text("全年通常最多选 46 学分。")
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起" else "查看与计算") }
            if (expanded) {
            Text("不及格课程也计入；集中讲义、毕业要求外课程及认定学分除外。")
            Text("这是选课登记额度，不是已修得学分上限。不能用“46 − 已通过学分”计算剩余额度。", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(registered, { registered = it }, label = { Text("本年度已登记的 CAP 计入学分") },
                supportingText = { Text("填写教务核对后的全年总数，包含未出成绩和不及格课程") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(planned, { planned = it }, label = { Text("拟新增的 CAP 计入学分") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(available, { available = it }, label = { Text("教务显示的当期可追加额度（选填）") },
                supportingText = { Text("填当前剩余额度，不填学期总上限；如计数口径不同，请直接以教务结果为准") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            if (result != null) {
                val message = buildString {
                    append("计划后全年 ${result.annualTotal.stripTrailingZeros().toPlainString()} / 46 学分。")
                    if (result.annualRemaining.signum() < 0) append("全年超出 ${result.annualRemaining.abs().stripTrailingZeros().toPlainString()} 学分，请减少计划或确认学校批准的特殊额度。")
                    else append("全年剩余 ${result.annualRemaining.stripTrailingZeros().toPlainString()} 学分。")
                    result.periodRemaining?.let {
                        append(if (it.signum() < 0) "当期额度超出 ${it.abs().stripTrailingZeros().toPlainString()} 学分。" else "当期额度剩余 ${it.stripTrailingZeros().toPlainString()} 学分。")
                    } ?: append("尚未核对当期额度。")
                }
                Text(message, color = if (result.exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            } else if (registered.isNotBlank() || planned.isNotBlank() || available.isNotBlank()) {
                Text("请填写有效的非负学分数，并补齐全年已登记与拟新增学分。", color = MaterialTheme.colorScheme.error)
            }
            Text("54 学分例外：申请时最近学期 GPA ≥ 2.75，并取得班级担任、学科教务委员的批准与指导，以及工学部教务委员会批准。App 不自动启用。", style = MaterialTheme.typography.bodySmall)
            Text("远程授课：最多 60 学分计入毕业要求，不能据此判断还能选多少课。已取得学分的课程原则上不能再次履修。", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(IbarakiCurriculum.sourceUrl))) }) { Text("查看学校规定（第 4–6 页）") }
            }
        }
    }
}

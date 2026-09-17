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
import com.tyust.course.academic.*

@Composable
fun EnrollmentRulesCard(grades: List<PortalGrade>? = null, scope: CurriculumScope = CurriculumScope("工学部", "情報工学科", 2026)) {
    val rules = remember(scope) { CurriculumConstraints.rules(scope) }
    if (rules.isEmpty()) return
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showChecks by rememberSaveable { mutableStateOf(false) }
    var index by rememberSaveable(scope) { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var existing by rememberSaveable(index) { mutableStateOf("") }
    var planned by rememberSaveable(index) { mutableStateOf("") }
    val rule = rules[index.coerceIn(rules.indices)]
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("选课规则", style = MaterialTheme.typography.titleLarge)
        Text("限修、先修、必修组合等 ${rules.size} 项规则。")
        TextButton(onClick = { showChecks = !showChecks }) { Text(if (showChecks) "收起自动核对" else "按已同步成绩核对必修") }
        if (showChecks) {
            Text("仅计已合格、分类明确的成绩；预计新增不计入。学分满足不代表审批、指定班级等条件已满足。", style = MaterialTheme.typography.bodySmall)
            CurriculumConstraintChecks.evaluate(scope, grades).forEach { check ->
                Text("${check.spec.title}：${if (grades == null) "—" else check.earned.stripTrailingZeros().toPlainString()}/${check.spec.minimum} · ${check.status}")
                Text(check.note, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起规则" else "查看规则") }
        if (expanded) {
            Text("适用：${scope.cohort} 入学 · ${scope.faculty} · ${scope.department}", style = MaterialTheme.typography.bodySmall)
            Box {
                OutlinedButton(onClick = { menu = true }) { Row { RawText("${index + 1}. "); Text(rule.title); RawText(" ▾") } }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                    rules.forEachIndexed { i, r -> DropdownMenuItem(text = { Row { RawText("${i + 1}. "); Text(r.title) } }, onClick = { index = i; menu = false }) }
                }
            }
            Text("核对范围：${rule.scope}", style = MaterialTheme.typography.titleSmall)
            Text(rule.detail)
            if (rule.limit != null) {
                Text("上限：${rule.limit.toPlainString()} ${rule.unit}")
                OutlinedTextField(existing, { existing = it }, label = { Text("上述范围内已有数量（${rule.unit}）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(planned, { planned = it }, label = { Text("拟增加数量（${rule.unit}）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                val remaining = runCatching { EnrollmentRules.remaining(rule, existing.toBigDecimal(), planned.toBigDecimal()) }.getOrNull()
                if (remaining != null) Text(if (remaining.signum() < 0) "计划超限 ${remaining.abs().stripTrailingZeros().toPlainString()} ${rule.unit}，请调整或核对学校例外安排。"
                    else "本项数量检查未超限，余量 ${remaining.stripTrailingZeros().toPlainString()} ${rule.unit}；还需满足其他履修条件。",
                    color = if (remaining.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                else if (existing.isNotBlank() || planned.isNotBlank()) Text("请填写两个非负数；门数必须为整数。", color = MaterialTheme.colorScheme.error)
                Text("按对应学期、学年或累计范围填写；不会把成绩发布学季当作实际履修学期。重修、审批例外需按学校结果核对。", style = MaterialTheme.typography.bodySmall)
            }
            if (rule.id == "english_prerequisite") Text(EnrollmentRules.englishStatus(grades.orEmpty()))
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rule.source))) }) { Text("查看依据：第 ${rule.page} 页") }
        }
    } }
}

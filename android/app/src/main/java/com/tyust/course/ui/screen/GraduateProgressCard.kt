package com.tyust.course.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.*
import com.tyust.course.i18n.LocalizedText as Text

@Composable
fun GraduateProgressCard(scope: CurriculumScope, grades: List<PortalGrade>?) {
    val rule = GraduateCurricula.rule(scope)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("大学院修了进度", style = MaterialTheme.typography.titleLarge)
        if(rule == null) {
            Text(GraduateCurricula.missing(scope))
        } else {
            Text("总要求：${rule.total} 学分 · 标准学制 ${rule.standardYears} 年", style = MaterialTheme.typography.titleMedium)
            if(grades == null) Text("尚未读取成绩，当前学分进度未知。")
            else {
                val progress = GraduateCurricula.progress(scope, grades)
                val root = progress.categories.single()
                Text("已确认归类 ${root.earned.stripTrailingZeros().toPlainString()} 学分 · 待分类 ${progress.unassigned.stripTrailingZeros().toPlainString()} 学分")
                Text(if(root.satisfied) "已匹配学分达到数值下限；修了资格仍待学校确认。"
                    else if(progress.unassigned.signum() > 0) "存在待分类学分，暂不能确认学分要求是否满足。"
                    else "已读取成绩尚未满足全部学分下限。")
            }
            GraduateRequirementRows(rule.requirements, grades?.let { GraduateCurricula.progress(scope,it).categories.single().children })
            Text("学分以外的条件", style = MaterialTheme.typography.titleMedium)
            rule.conditions.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            Text(rule.notes, style = MaterialTheme.typography.bodySmall)
        }
    } }
}

@Composable
private fun GraduateRequirementRows(requirements: List<CreditRequirement>, progress: List<CreditProgress>?) {
    requirements.forEach { r ->
        val p = progress?.singleOrNull { it.requirement.id == r.id }
        Column(Modifier.fillMaxWidth().padding(start=if(r.children.isEmpty()) 8.dp else 0.dp)) {
            Text(r.label, style=MaterialTheme.typography.bodyMedium)
            Text(if(p == null) "已修待读取 · 最低 ${r.required.stripTrailingZeros().toPlainString()} 学分" else
                "已确认 ${p.earned.stripTrailingZeros().toPlainString()} · 最低 ${r.required.stripTrailingZeros().toPlainString()} 学分", style=MaterialTheme.typography.bodySmall)
            if(r.children.isNotEmpty()) GraduateRequirementRows(r.children,p?.children)
        }
    }
}

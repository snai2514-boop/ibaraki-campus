package com.tyust.course.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.*
import com.tyust.course.i18n.LocalizedText as Text
import androidx.compose.material3.Text as RawText
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CurriculumCard(scope: CurriculumScope?, store: CurriculumSelectionStore?, manuallySelected: Boolean,
    onSelect: (CurriculumScope?) -> Unit) {
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    var edit by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val summary = scope?.let(UniversityCurricula::summary)
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri != null && scope != null && store != null) coroutine.launch {
            saving = true
            message = try { withContext(Dispatchers.IO) { store.saveEvidence(scope, uri) }; "文件已保存到本机，尚未用于计算。" }
            catch(e: Exception) { "文件保存失败，请选择 32 MB 以内的 PDF。" }
            finally { saving = false }
        }
    }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if(scope?.let(GraduateCurricula::isGraduate) == true) "修了规则" else "毕业规则", style = MaterialTheme.typography.titleMedium)
        if(scope != null) {
            RawText(listOf(scope.faculty, scope.department, scope.program).filter { it.isNotBlank() }.distinct().joinToString(" · "))
            Text(if(scope.cohort == null) "规则年度未读取，请更新资料或选择。" else "规则年度：${scope.cohort}")
            Text(if(manuallySelected) "使用自行选择的规则" else "根据学校学籍匹配", style = MaterialTheme.typography.bodySmall)
        }
        if(scope?.let(GraduateCurricula::isGraduate) == true) {
            Text(scope.level.label + if(scope.track.isBlank()) "" else " · ${scope.track}")
            if(GraduateCurricula.rule(scope) == null) Text(GraduateCurricula.missing(scope))
        }
        else if(summary == null) Text("毕业要求待核对，学校成绩正常保留。")
        else if(!summary.detailedInformation2026) {
            Text("总要求：${summary.total} 学分", style = MaterialTheme.typography.titleMedium)
            summary.rows.forEach { (label, credits) -> Row(Modifier.fillMaxWidth()) {
                RawText(label, Modifier.weight(1f)); Text("$credits 学分")
            } }
            Text("以上为最低学分要求，必修课程及附加条件须同时满足。", style = MaterialTheme.typography.bodySmall)
            if(summary.detail.isNotBlank()) Text(summary.detail, style = MaterialTheme.typography.bodySmall)
        }
        if(scope != null) TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(if(GraduateCurricula.isGraduate(scope)) GraduateCurricula.guide(scope) else summary?.source ?: UniversityCurricula.guide(scope))))
        }) { Text("查看学校履修要项") }
        Row {
            TextButton(enabled = store != null && !saving, onClick = { edit = true }) { Text("选择适用规则") }
            TextButton(enabled = store != null && scope?.cohort != null && !saving, onClick = { pickPdf.launch(arrayOf("application/pdf")) }) { Text("补充履修要项") }
        }
        Text("补充文件仅保存在本机，核对适用年度与方向后再适配。", style = MaterialTheme.typography.bodySmall)
        if(scope != null && store?.hasEvidence(scope) == true) Text("已有本机补充文件")
        if(message.isNotBlank()) Text(message)
        if(saving) LinearProgressIndicator(Modifier.fillMaxWidth())
    } }
    if(edit) CurriculumDialog(scope, onDismiss = { edit = false }, onSave = { onSelect(it); edit = false })
}

@Composable
private fun CurriculumDialog(initial: CurriculumScope?, onDismiss: () -> Unit, onSave: (CurriculumScope?) -> Unit) {
    val faculties = UniversityCurricula.departments + GraduateCurricula.departments
    var faculty by remember { mutableStateOf(initial?.faculty?.takeIf { it in faculties }.orEmpty()) }
    var department by remember { mutableStateOf(initial?.department.orEmpty()) }
    var cohort by remember { mutableStateOf(initial?.cohort?.toString().orEmpty()) }
    var program by remember { mutableStateOf(initial?.program.orEmpty()) }
    var level by remember { mutableStateOf(initial?.level ?: StudyLevel.UNDERGRADUATE) }
    var track by remember { mutableStateOf(initial?.track.orEmpty()) }
    val current = CurriculumScope(faculty, department, cohort.toIntOrNull(), program, level, track)
    val graduate = faculty in GraduateCurricula.departments
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择适用规则") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("按学校的要件年度选择，不能用当前年级倒推。")
            Choice("学部／学環／研究科", faculty, faculties.keys.toList()) { faculty=it; department=""; program=""; track=""; level=if(it in GraduateCurricula.departments) StudyLevel.UNKNOWN else StudyLevel.UNDERGRADUATE }
            if(graduate) Choice("学位层次／身份", level.label, GraduateCurricula.levels(faculty).map { it.label }) {
                level=StudyLevel.entries.single { l -> l.label == it }; department=""; program=""; track=""
            }
            Choice("学科／专攻", department, if(graduate) GraduateCurricula.majors(faculty, level) else faculties[faculty].orEmpty()) { department=it; program=""; track="" }
            OutlinedTextField(cohort, { if(it.length<=4 && it.all(Char::isDigit)) cohort=it }, label={ Text("规则年度") }, singleLine=true)
            Choice("コース／プログラム", program, listOf("") + if(graduate) GraduateCurricula.programs(current) else UniversityCurricula.programs(current)) { program=it; track="" }
            if(graduate && GraduateCurricula.tracks(current).isNotEmpty()) Choice("履修类型", track, listOf("") + GraduateCurricula.tracks(current)) { track=it }
            OutlinedTextField(program, { program=it.take(120) }, label={ Text("专业方向（可补充）") })
            TextButton(onClick={ onSave(null) }) { Text("恢复学校学籍匹配") }
        }
    }, confirmButton = {
        TextButton(enabled=department in faculties[faculty].orEmpty() && current.cohort in 1900..2099,
            onClick={ onSave(current) }) { Text("保存") }
    }, dismissButton={ TextButton(onClick=onDismiss) { Text("取消") } })
}

@Composable
private fun Choice(label: String, value: String, values: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box { OutlinedButton(onClick={ expanded=true }) { RawText(value.ifBlank { "—" }) }
        DropdownMenu(expanded, onDismissRequest={ expanded=false }, modifier=Modifier.heightIn(max=320.dp)) {
            values.forEach { option -> DropdownMenuItem(text={ RawText(option.ifBlank { "—" }) }, onClick={ onSelect(option); expanded=false }) }
        }
        }
    }
}

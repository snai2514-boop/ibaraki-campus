package com.tyust.course.ui.screen

import com.tyust.course.i18n.LocalizedText as Text
import androidx.compose.material3.Text as RawText


import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.*
import com.tyust.course.ui.system.GlassPageScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StudyProgressScreen(onBack: (() -> Unit)? = null) {
    var school by rememberSaveable { mutableStateOf(true) }
    if (school) { SchoolDataScreen(true, { school = false }, onBack); return }
    val context = LocalContext.current
    val store = remember { StudyRecordStore(context) }
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<StudyRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    val editor = records.find { it.courseId == editorId }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<StudyRecord?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(revision) {
        loading = true
        try { records = withContext(Dispatchers.IO) { store.load() }; loadFailed = false; error = null }
        catch (e: Exception) { loadFailed = true; error = "记录读取失败，原文件已保留。请重试。" }
        finally { loading = false }
    }
    fun save(next: List<StudyRecord>) {
        if (saving || loading || loadFailed) return
        saving = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { store.save(next) }
                records = next; showEditor = false; deleting = null; error = null
            } catch (e: Exception) { error = "保存失败，修改尚未写入，请重试。" }
            finally { saving = false }
        }
    }
    fun open(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { error = "无法打开网页，请检查是否已安装浏览器。" }
    }
    val progress = remember(records) { IbarakiStudyCalculator.progress(records) }
    val gpa = remember(records) { IbarakiStudyCalculator.gpa(records) }
    GlassPageScaffold(title = "学业进度", subtitle = "2026 入学 · 工学部 情報工学科", onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextButton(onClick = { school = true }) { Text("查看学校成绩与进度") }
                Text("本机手动记录 · 尚未同步学校", style = MaterialTheme.typography.labelLarge)
                Text("分类进度用于核对已记录学分；必修课程、认定与毕业资格仍须按学校要求确认。", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { context.startActivity(Intent(context, com.tyust.course.IbarakiTimetableActivity::class.java)) }) {
                    Text("打开茨城大学课表与日历")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("分类进度", "成绩记录", "学校指南").forEachIndexed { index, label ->
                        FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(label) })
                    }
                }
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item { EnrollmentCapCard() }
            item { EnrollmentRulesCard() }
            error?.let { message -> item {
                Text(message, color = MaterialTheme.colorScheme.error)
                if (loadFailed) TextButton(onClick = { revision++ }) { Text("重新读取") }
            } }
            if (!loading && !loadFailed) {
                if (tab == 0) {
                    item {
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("已录入成绩的通算 GPA", style = MaterialTheme.typography.titleMedium)
                            Text(gpa?.toPlainString() ?: "尚无可计算成绩", style = MaterialTheme.typography.headlineMedium)
                            Text("本机计算值，非官方同步。仅根据已录入的最新成绩计算，缺少记录会影响结果。", style = MaterialTheme.typography.bodySmall)
                            Text("已获学分中尚未分类：${progress.unassigned.stripTrailingZeros().toPlainString()}", style = MaterialTheme.typography.bodyMedium)
                        } }
                    }
                    item { ProgressBranch(progress.categories.single()) }
                    item {
                        Text("选课建议", style = MaterialTheme.typography.titleLarge)
                        Text("依据已记录学分提示缺额类别。尚无当期开课、先修与时间冲突数据，以下不是具体课程的可选确认。", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { open("https://syllabus.ibaraki.ac.jp/") }) { Text("查询学校官方课程目录") }
                        if (records.isEmpty()) Text("先录入已有成绩，建议才会反映你的实际缺额。")
                    }
                    items(IbarakiStudyCalculator.suggestions(records), key = { "suggestion-${it.requirement.id}" }) { suggestion ->
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                            Text("${suggestion.requirement.label} · 还差 ${suggestion.remaining.stripTrailingZeros().toPlainString()} 学分", style = MaterialTheme.typography.titleSmall)
                            Text(if (suggestion.requirement.id == "thesis") "毕业研究需先满足着手条件，请查看官方要项第 20 页。" else "优先核对该类别的必修与可选课程，再确认当期开课、先修条件及时间。", style = MaterialTheme.typography.bodySmall)
                        } }
                    }
                    item { Text("多文化理解包括多文化コミュニケーション与ヒューマニティーズ。选择履修超出部分是否转入自由履修，请按学校认定填写；不会自动把其他类别的缺额抵消。", style = MaterialTheme.typography.bodySmall) }
                }
                if (tab == 1) {
                    item {
                        Button(onClick = { editorId = null; showEditor = true }, enabled = !saving) { Text("添加课程成绩") }
                        Text("同一课程只保留最新结果；重修后编辑原记录。课程代码用于避免重复计数。分类不确定时可先选未分类。", style = MaterialTheme.typography.bodySmall)
                    }
                    if (records.isEmpty()) item { Text("还没有成绩记录。添加后即可查看 GPA 和各类学分进度。") }
                    items(records, key = { it.courseId }) { record ->
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                            RawText(record.name, style = MaterialTheme.typography.titleMedium)
                            Text("${record.courseId} · ${record.credits.stripTrailingZeros().toPlainString()} 学分 · ${record.score?.let { "$it 分" } ?: record.kind.label}")
                            Text(IbarakiCurriculum.leaves.find { it.id == record.categoryId }?.label ?: "未分类", style = MaterialTheme.typography.bodySmall)
                            Row {
                                TextButton(onClick = { editorId = record.courseId; showEditor = true }, enabled = !saving) { Text("修改") }
                                TextButton(onClick = { deleting = record }, enabled = !saving) { Text("删除") }
                            }
                        } }
                    }
                }
                if (tab == 2) {
                    item {
                        Text("毕业要求与 GPA", style = MaterialTheme.typography.titleLarge)
                        Text("2026 入学者履修要项：第 3 页为总体分类，第 20 页为信息工学科要求，第 49–50 页为 GPA。")
                        OutlinedButton(onClick = { open(IbarakiCurriculum.sourceUrl) }) { Text("打开官方履修要项") }
                        Text("百分制 GP =（分数 − 55）÷ 10；低于 60 分的 GP 为 0。不及格课程仍计入 GPA 分母。认定学分、学外实习和毕业要求外课程不计入 GPA。", style = MaterialTheme.typography.bodyMedium)
                    }
                    item {
                        Text("选课与退课：官方网页指导", style = MaterialTheme.typography.titleLarge)
                        Text("按学校当期通知办理。抽选申请使用学校发布的链接；教务门户的登记与修正期结束后，官方指南说明须取得任课教师批准并按对应表单申请。")
                        Text("在修课表中的删除不等于学校退课。本页提供官方指南入口，实际办理请在学校网页完成。")
                        OutlinedButton(onClick = { open(IbarakiCurriculum.guidanceUrl) }) { Text("打开学校履修与选退课指南") }
                    }
                }
            }
        }
    }
    if (showEditor && !loading && !loadFailed) key(editor?.courseId) {
        StudyRecordEditor(editor, records, saving, saveError = error, onDismiss = { if (!saving) showEditor = false }, onSave = { record ->
            save(records.filterNot { it.courseId == editor?.courseId } + record)
        })
    }
    deleting?.let { record -> AlertDialog(onDismissRequest = { if (!saving) deleting = null }, title = { Text("删除本机记录？") },
        text = { Text("删除「${record.name}」将重新计算本机进度，不影响学校记录。") },
        confirmButton = { TextButton(onClick = { save(records.filterNot { it.courseId == record.courseId }) }, enabled = !saving) { Text("删除") } },
        dismissButton = { TextButton(onClick = { deleting = null }, enabled = !saving) { Text("取消") } }) }
}

@Composable
internal fun ProgressBranch(progress: CreditProgress, depth: Int = 0) {
    val hasMinimum = progress.requirement.required.signum() > 0
    Column(Modifier.fillMaxWidth().padding(start = if (depth > 0) 12.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(progress.requirement.label, Modifier.weight(1f), style = if (depth == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium)
            Text(if (hasMinimum) progress.display else "已修 ${progress.earned.stripTrailingZeros().toPlainString()}", style = MaterialTheme.typography.titleMedium)
        }
        if (hasMinimum) LinearProgressIndicator(progress = { (progress.earned.toFloat() / progress.requirement.required.toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        Text(if (!hasMinimum) "计入上级类别" else if (progress.satisfied) "数量已达标" else "还差 ${progress.remaining.stripTrailingZeros().toPlainString()} 学分${if (progress.remaining.signum() == 0) "，子项未达标" else ""}", style = MaterialTheme.typography.labelSmall)
        progress.children.forEach { ProgressBranch(it, depth + 1) }
        if (depth == 1) HorizontalDivider()
    }
}

@Composable
private fun StudyRecordEditor(original: StudyRecord?, records: List<StudyRecord>, saving: Boolean, saveError: String?, onDismiss: () -> Unit, onSave: (StudyRecord) -> Unit) {
    var id by rememberSaveable { mutableStateOf(original?.courseId ?: "") }
    var name by rememberSaveable { mutableStateOf(original?.name ?: "") }
    var credits by rememberSaveable { mutableStateOf(original?.credits?.toPlainString() ?: "") }
    var score by rememberSaveable { mutableStateOf(original?.score?.toString() ?: "") }
    var kindName by rememberSaveable { mutableStateOf((original?.kind ?: StudyGradeKind.SCORED).name) }
    var category by rememberSaveable { mutableStateOf(original?.categoryId ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val kind = StudyGradeKind.valueOf(kindName)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (original == null) "添加成绩" else "修改最新成绩") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(id, { id = it }, label = { Text("课程代码") }, singleLine = true, enabled = !saving)
            OutlinedTextField(name, { name = it }, label = { Text("课程名称") }, singleLine = true, enabled = !saving)
            OutlinedTextField(credits, { credits = it }, label = { Text("学分，例如 2") }, singleLine = true, enabled = !saving)
            StudyChoice("成绩类型", kind.label, StudyGradeKind.entries.map { it.name to it.label }, !saving) { kindName = it }
            if (kind == StudyGradeKind.SCORED) OutlinedTextField(score, { score = it }, label = { Text("百分制成绩，0–100") }, singleLine = true, enabled = !saving)
            StudyChoice("毕业学分类别", IbarakiCurriculum.leaves.find { it.id == category }?.label ?: "未分类",
                listOf("" to "未分类") + IbarakiCurriculum.leaves.map { it.id to it.label }, !saving) { category = it }
            if (kind == StudyGradeKind.INTERNSHIP) Text("仅在已获得学分后选择学外实习；尚未完成请选择在修。")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !saving, onClick = {
            try {
                val record = StudyRecord(id.trim(), name.trim(), credits.trim().toBigDecimalOrNull() ?: error("请填写有效学分"),
                    if (kind == StudyGradeKind.SCORED) score.trim().toIntOrNull() ?: error("请填写整数分数") else null, kind, category.ifBlank { null })
                record.validate()
                require(records.none { it.courseId == record.courseId && it.courseId != original?.courseId }) { "此课程已存在，请修改已有记录" }
                onSave(record)
            } catch (e: Exception) { error = e.message ?: "请检查输入" }
        }) { Text(if (saving) "保存中…" else "保存") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("取消") } })
}

@Composable
private fun StudyChoice(label: String, selected: String, choices: List<Pair<String, String>>, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text("$label：$selected") }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false }) }
        }
    }
}

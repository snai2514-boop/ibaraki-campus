package com.tyust.course.ui.screen

import com.tyust.course.i18n.LocalizedText as Text
import androidx.compose.material3.Text as RawText


import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.*
import com.tyust.course.ui.system.GlassPageScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SchoolDataScreen(grades: Boolean, onManual: () -> Unit, onBack: (() -> Unit)?) {
    val context = LocalContext.current
    var snapshots by remember { mutableStateOf(SchoolSyncState.snapshots.value.filter { if (grades) it.grades.isNotEmpty() else it.lessons.isNotEmpty() }) }
    var forecastTables by remember { mutableStateOf(SchoolSyncState.snapshots.value.filter { it.lessons.isNotEmpty() }) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var courseCatalog by remember { mutableStateOf(UniversityCourseCatalog.empty) }
    var calendar by remember { mutableStateOf(true) }
    var revision by remember { mutableIntStateOf(0) }
    val dataRevision by SchoolSyncState.dataRevision.collectAsState()
    val syncing by SchoolSyncState.running.collectAsState()
    val syncMessage by SchoolSyncState.message.collectAsState()
    DisposableEffect(context) {
        val lifecycle = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) revision++
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(revision, dataRevision) {
        loading = true; error = null
        try {
            val previousKey = snapshots.getOrNull(selected)?.key
            val all = withContext(Dispatchers.IO) {
                courseCatalog = UniversityCourseCatalogStore.load(context)
                SchoolImportStore(context).load()
            }
            forecastTables = all.filter { it.lessons.isNotEmpty() }
            snapshots = all.filter { if (grades) it.grades.isNotEmpty() else it.lessons.isNotEmpty() }
            val previousIndex = snapshots.indexOfFirst { it.key == previousKey }
            if (previousIndex >= 0) selected = previousIndex
            else if (!grades && snapshots.isNotEmpty()) {
                val today = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tokyo"))
                val monthDay = (today.get(java.util.Calendar.MONTH) + 1) * 100 + today.get(java.util.Calendar.DAY_OF_MONTH)
                val year = today.get(java.util.Calendar.YEAR) - if (monthDay < 410) 1 else 0
                val quarter = when { monthDay < 410 -> 4; monthDay < 604 -> 1; monthDay < 925 -> 2; monthDay < 1125 -> 3; else -> 4 }
                val desired = "timetable-$year-Q$quarter"
                val preferred = snapshots.filter { it.key.substringAfterLast('/') <= desired }.maxByOrNull { it.key.substringAfterLast('/') }
                selected = snapshots.indexOf(preferred).coerceAtLeast(0)
            } else selected = 0
        }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { error = "学校记录读取失败，原文件已保留，请重新读取学校页面。" }
        finally { loading = false }
    }
    val item = snapshots.getOrNull(selected)
    val liveProfile by SchoolSyncState.profile.collectAsState()
    var savedProfile by remember { mutableStateOf<SchoolStudentProfile?>(null) }
    LaunchedEffect(revision, liveProfile) {
        savedProfile = liveProfile ?: withContext(Dispatchers.IO) { runCatching { SchoolStudentProfileStore(context).load() }.getOrNull() }
    }
    val profile = savedProfile?.takeIf { item == null || UniversityCurricula.profileMatches(it, item.key) }
    val selectionStore = remember(profile?.studentNumber) { profile?.let { CurriculumSelectionStore(context, UniversityCurricula.owner(it)) } }
    var selectionRevision by remember { mutableIntStateOf(0) }
    val override = remember(selectionStore, selectionRevision) { runCatching { selectionStore?.load() }.getOrNull() }
    val ruleScope = override ?: profile?.let(UniversityCurricula::scope)
    val ruleSummary = ruleScope?.let(UniversityCurricula::summary)
    val detailedRules = ruleSummary?.detailedInformation2026 == true

    GlassPageScaffold(title = if (grades) "学分与 GPA" else "学校课表", subtitle = "已保存到本机", onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !syncing, onClick = { context.startActivity(Intent(context, com.tyust.course.IbarakiPortalActivity::class.java).putExtra("gradesOnly", grades)) }) { Text(if (syncing) "后台同步中" else "资料更新") }
                }
                Text("更新失败时保留上次数据。", style = MaterialTheme.typography.bodySmall)
                if (syncMessage.isNotBlank()) Text(syncMessage, style = MaterialTheme.typography.bodySmall)
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (grades && item != null) item { SchoolCreditForecastCard(item, forecastTables, profile?.faculty) }
            if (grades) item { CurriculumCard(ruleScope, selectionStore, override != null) {
                runCatching { if(it == null) selectionStore?.reset() else selectionStore?.save(it) }
                    .onFailure { error = "规则保存失败，原设置已保留。" }
                selectionRevision++
            } }
            if (grades && detailedRules) item { EnrollmentCapCard() }
            if (grades && detailedRules) item { EnrollmentRulesCard(item?.grades.orEmpty()) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (!loading && snapshots.isEmpty() && error == null) item { Text("暂无数据，请点击“资料更新”。") }
            items(snapshots.indices.toList()) { i ->
                FilterChip(selected = selected == i, onClick = { selected = i }, label = {
                    Text(snapshots[i].title + if (snapshots.map { it.key.substringBefore('/') }.distinct().size > 1) " · 账户 ${snapshots[i].key.take(6)}" else "")
                })
            }
            if (item != null && item.syncedAt > 0) item {
                Text("上次同步：" + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ROOT).format(java.util.Date(item.syncedAt)), style = MaterialTheme.typography.bodySmall)
            }
            if (item != null && grades) {
                item { Text(item.cards.first().replace("学校通算 GPA：", "累计 GPA：").substringBefore("（"), style = MaterialTheme.typography.titleLarge) }
                val earned = item.grades.filter { it.passed }.fold(java.math.BigDecimal.ZERO) { sum, g -> sum + g.credits }
                item { Text("已修学分：${earned.stripTrailingZeros().toPlainString()}", style = MaterialTheme.typography.titleLarge) }
                if (ruleScope != null && ruleSummary != null) {
                val progress = UniversityCourseClassification.progress(ruleScope, item, courseCatalog)
                val root = progress.categories.single()
                item {
                    Text("毕业要求：${ruleSummary.total} 学分 + 分类与必修要求。", style = MaterialTheme.typography.bodySmall)
                    Text("毕业进度", style = MaterialTheme.typography.titleLarge)
                    Text("已分类 ${root.earned.stripTrailingZeros().toPlainString()} 学分 · 待分类 ${progress.unassigned.stripTrailingZeros().toPlainString()} 学分")
                    if (progress.unassigned.signum() > 0) Text("待分类学分已计入总分，类别尚待确认。")
                    Text("${ruleScope.cohort} · ${ruleScope.faculty} · ${ruleScope.department} ${ruleScope.program}", style = MaterialTheme.typography.bodySmall)
                }
                items(root.children) { branch -> ProgressBranch(branch) }
                if (progress.unassigned.signum() > 0) item {
                    Text("待分类课程", style = MaterialTheme.typography.titleMedium)
                    item.grades.filter { it.passed && UniversityCourseClassification.classify(ruleScope, it.name, it.category, courseCatalog, credits = it.credits).let { c -> c.categoryId == null && !c.excluded } }.forEach {
                        Text("${it.name} · ${it.credits.stripTrailingZeros().toPlainString()} 学分")
                    }
                }
                } else item { Text("课程分类按学校原文显示，毕业归类尚待核对。") }
                item { Text("课程成绩", style = MaterialTheme.typography.titleLarge) }
                items(item.grades) { g ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                        RawText(g.name, style = MaterialTheme.typography.titleMedium)
                        Text("${g.credits} 学分 · ${g.score?.toString() ?: "无百分制分数"} · ${g.grade} · ${if (g.passed) "通过" else "未通过"}")
                        Text("${g.year} · ${g.term}\n${g.category}", style = MaterialTheme.typography.bodySmall)
                        if (ruleScope != null && ruleSummary != null) {
                        val classification = UniversityCourseClassification.classify(ruleScope, g.name, g.category, courseCatalog, credits = g.credits)
                        Text(if (!g.passed) "未通过 · 不计学分" else "毕业类别：" +
                            classification.label,
                            style = MaterialTheme.typography.bodySmall)
                        if (classification.source.startsWith("https://")) TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(classification.source)))
                        }) { Text("查看分类依据") }
                        }
                    } }
                }
            }
            if (item != null && !grades) {
                val calendarDetails = SchoolAcademicCalendar.resolve(item, profile)
                val calendarResult = runCatching { calendarDetails.events.also { require(it.isNotEmpty()) } }
                val termLabel = item.key.substringAfterLast('/').removePrefix("timetable-")
                item {
                    Text("按所选学季显示；历史课表不会变成当前学季。")
                    if (calendarResult.isSuccess) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { calendar = !calendar }) { Text(if (calendar) "查看星期课表" else "查看学季日历") }
                            TextButton(enabled = calendarDetails.issues.isEmpty(), onClick = {
                                runCatching {
                                    val file = java.io.File(context.cacheDir, "ibaraki-$termLabel.ics")
                                    file.writeText(SchoolQ2Calendar.ics(item, profile), Charsets.UTF_8)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                        type = "text/calendar"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }, "导出学季日历"))
                                }.onFailure { error = "日历导出失败，请重试。" }
                            }) { Text("导出日历") }
                        }
                        Text("按官方 2026 学年度对应校区校历生成 ${calendarResult.getOrThrow().size} 次计划。仅 $termLabel 范围；不包含预备日、教师临时调课。考试可能日已标注。时间为东京时间，随课表自动更新。", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(SchoolQ2Calendar.source))) }) { Text("查看校历依据") }
                    } else Text("本学季或部分课程的授课日期尚未核对，暂不能生成日历。")
                    calendarDetails.issues.forEach { issue -> Text("${issue.lesson.description}：${issue.reason}", style = MaterialTheme.typography.bodySmall) }
                }
                if (calendar && calendarResult.isSuccess) {
                    items(calendarResult.getOrThrow()) { event -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                        Text("${event.date} · 第 ${event.lesson.period} 限", style = MaterialTheme.typography.titleMedium)
                        RawText(event.lesson.description)
                        if (event.examPossible) Text("考试可能日，具体安排以教师通知为准", style = MaterialTheme.typography.bodySmall)
                    } } }
                } else {
                for (day in 1..6) {
                    val lessons = item.lessons.filter { it.day == day }.sortedBy { it.period }
                    if (lessons.isNotEmpty()) {
                        item { Text("星期${"一二三四五六"[day - 1]}", style = MaterialTheme.typography.titleLarge) }
                        items(lessons) { lesson -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                            Text("第 ${lesson.period} 限", style = MaterialTheme.typography.titleMedium)
                            RawText(lesson.description)
                        } } }
                    }
                }
                }
            }
        }
    }
}

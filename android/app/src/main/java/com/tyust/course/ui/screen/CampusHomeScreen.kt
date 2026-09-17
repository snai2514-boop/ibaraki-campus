package com.tyust.course.ui.screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyust.course.R
import com.tyust.course.IbarakiAccountActivity
import com.tyust.course.IbarakiPortalActivity
import com.tyust.course.academic.*
import com.tyust.course.i18n.LocalizedText as Text
import com.tyust.course.i18n.LanguageButton
import com.tyust.course.i18n.SupportFooter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Navigation owns presentation only; school authentication and stored records remain unchanged. */
@Composable
fun CampusHomeScreen() {
    var page by rememberSaveable { mutableStateOf("首页") }
    var preview by rememberSaveable { mutableStateOf(false) }
    val phoneDate = rememberPhoneDate()
    val currentYear = phoneDate.take(4).toInt() - if (phoneDate.substring(5, 7).toInt() < 4) 1 else 0
    var previewYear by rememberSaveable { mutableIntStateOf(currentYear) }
    var previewSecondHalf by rememberSaveable { mutableStateOf(phoneDate.substring(5, 7).toInt() !in 4..9) }
    var detail by rememberSaveable { mutableStateOf(false) }
    var theme by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var registrationRequested by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val activity = context as? android.app.Activity
        fun openRequested() {
            if(activity?.intent?.getBooleanExtra("openNotices", false) == true) { page = "通知"; detail = false; activity.intent.removeExtra("openNotices") }
            if(activity?.intent?.getBooleanExtra("openRegistration", false) == true) {registrationRequested=true; activity.intent.removeExtra("openRegistration")}
        }
        openRequested()
        val lifecycle = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if(event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) openRequested() }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val profile by SchoolSyncState.profile.collectAsState()
    val syncing by SchoolSyncState.running.collectAsState()
    val registrationRevision by SchoolRegistrationStore.revision.collectAsState()
    val registrationStore = remember(profile?.studentNumber) { profile?.let { SchoolRegistrationStore(context, it.studentNumber) } }
    var registrationAlert by remember(profile?.studentNumber) { mutableStateOf("") }
    LaunchedEffect(registrationRevision, profile?.studentNumber) { registrationAlert = registrationStore?.alert().orEmpty() }
    fun openRegistration() { context.startActivity(Intent(context, com.tyust.course.IbarakiRegistrationActivity::class.java)) }
    LaunchedEffect(syncing, registrationRequested, profile?.studentNumber) {
        if(!syncing && profile!=null && registrationRequested) {
            registrationRequested=false; openRegistration()
        }
    }
    if(registrationAlert.isNotBlank()) AlertDialog(onDismissRequest={registrationStore?.acknowledge(); registrationAlert=""},
        title={Text("发现可登录课程")},text={Text(registrationAlert)},
        confirmButton={TextButton(enabled=!syncing,onClick={registrationStore?.acknowledge(); registrationAlert=""; openRegistration()}) {Text("查看可登录课程")}},
        dismissButton={TextButton(onClick={registrationStore?.acknowledge(); registrationAlert=""}) {Text("稍后查看")}})
    val dataRevision by SchoolSyncState.dataRevision.collectAsState()
    val syncMessage by SchoolSyncState.message.collectAsState()
    val syncDetail by SchoolSyncState.details.collectAsState()
    var syncInfo by remember { mutableStateOf(false) }
    var records by remember { mutableStateOf(SchoolSyncState.snapshots.value) }
    var courseCatalog by remember { mutableStateOf(UniversityCourseCatalog.empty) }
    var readError by remember { mutableStateOf(false) }
    LaunchedEffect(dataRevision, profile?.studentNumber) {
        runCatching { withContext(Dispatchers.IO) {
            courseCatalog = UniversityCourseCatalogStore.load(context)
            SchoolImportStore(context).load()
        } }
            .onSuccess { all -> records = all.filter { profile == null || UniversityCurricula.profileMatches(profile!!, it.key) }; readError = false }
            .onFailure { readError = true }
    }
    val tabs = listOf("首页", "时间表", "课程", "学分", "设置")
    fun navigate(target: String) { page = target; detail = false }
    fun update() { if (!syncing) context.startActivity(Intent(context, IbarakiPortalActivity::class.java).putExtra("backgroundSync", true).putExtra("gradesOnly", page == "学分")) }
    BackHandler(page != "首页" || detail) { if (detail) detail = false else page = "首页" }
    val dark = MaterialTheme.colorScheme.background.luminanceForCampus() < .4f
    val colors = if (dark) MaterialTheme.colorScheme else MaterialTheme.colorScheme.copy(
        background = Color(0xFFF2F8FF), surface = Color.White,
        surfaceVariant = Color(0xFFEAF3FC), primary = Color(0xFF147DF1),
        onSurface = Color(0xFF172D50), onBackground = Color(0xFF172D50),
        primaryContainer = Color(0xFFDEEEFF), onSurfaceVariant = Color(0xFF526A86))
    MaterialTheme(colorScheme = colors) {
        Scaffold(containerColor = colors.background,
            bottomBar = {
                NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                    tabs.forEach { name -> NavigationBarItem(selected = page == name,
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primaryContainer),
                        onClick = { navigate(name) }, icon = { Icon(campusIcon(name), null) },
                        label = { Text(name, fontSize = 11.sp, maxLines = 1) }) }
                }
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                when {
                    detail -> SchoolDataScreen(grades = true, onManual = {}, onBack = { detail = false })
                    page == "时间表" -> SchoolTimetableScreen(onBack = { navigate("首页") })
                    else -> LazyColumn(Modifier.fillMaxSize().statusBarsPadding(),
                        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(if (page == "首页") "茨城大学" else page, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    if (page == "首页") Text("教务助手", color = colors.onSurfaceVariant, fontSize = 14.sp)
                                }
                                LanguageButton()
                            }
                        }
                        if (readError) item { Text("学校记录读取失败，原文件已保留，请重新读取学校页面。", color = colors.error) }
                        when (page) {
                            "首页" -> {
                                item { Button(onClick={openRegistration()},enabled=profile!=null && !syncing,modifier=Modifier.fillMaxWidth()) {Text("可登录课程")} }
                                item { CampusIllustration(Modifier.fillMaxWidth().height(120.dp)) }
                                if (profile != null) item {
                                    CampusCard {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(profile!!.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                            TextButton(onClick = { update() }, enabled = !syncing) { Text(if (syncing) "后台同步中" else "更新资料", fontSize = 12.sp) }
                                        }
                                        Text(listOf(profile!!.faculty, profile!!.department, profile!!.year).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 13.sp)
                                        Text(profile!!.studentNumber, color = colors.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }
                                SchoolHolidays.classroomNotice(phoneDate)?.let { notice -> item { Text(notice, fontSize = 12.sp) } }
                                if (profile == null) item { CampusRow("更新资料", Icons.Outlined.Refresh) { update() } }
                                if (syncMessage.isNotBlank()) item { TextButton(onClick = { syncInfo = true }) { Text(if (syncing) "后台同步中，可正常使用 · 查看详情" else "同步状态") } }
                                listOf(listOf("课程", "学分"), listOf("时间表", "通知"), listOf("常用链接", "设置")).forEach { row ->
                                    item { Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        row.forEach { target ->
                                            Card(onClick = { navigate(target) }, modifier = Modifier.weight(1f).heightIn(min = 104.dp),
                                                shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
                                                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                    Icon(campusIcon(target), null, Modifier.size(32.dp), tint = when(target) { "学分" -> Color(0xFF28A9A0); "通知" -> Color(0xFF8574DF); else -> colors.primary })
                                                    Row(verticalAlignment = Alignment.CenterVertically) { Text(campusTitle(target), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp)) }
                                                }
                                            }
                                        }
                                    } }
                                }
                            }
                            "课程" -> item { CampusCourseList(records) }
                            "学分" -> {
                                item {
                                    TextButton(enabled = !syncing, onClick = { update() }) { Text(if (syncing) "后台同步中" else "更新成绩与学分") }
                                    if (syncMessage.isNotBlank()) TextButton(onClick = { syncInfo = true }) { Text("同步状态") }
                                }
                                val grades = records.filter { it.grades.isNotEmpty() }.maxByOrNull { it.syncedAt }
                                val earned = grades?.grades?.filter { it.passed }?.fold(java.math.BigDecimal.ZERO) { sum, g -> sum + g.credits }
                                val scope = profile?.let { p -> runCatching { CurriculumSelectionStore(context, UniversityCurricula.owner(p)).load() }.getOrNull() ?: UniversityCurricula.scope(p) }
                                val requirement = scope?.let(UniversityCurricula::summary)
                                val ratio = if (earned != null && requirement != null) (earned.toFloat() / requirement.total).coerceIn(0f, 1f) else 0f
                                val earnedText = earned?.stripTrailingZeros()?.toPlainString() ?: "—"
                                val totalText = requirement?.total?.toString() ?: "—"
                                val remainingText = if (earned != null && requirement != null) (requirement.total.toBigDecimal() - earned).max(java.math.BigDecimal.ZERO).stripTrailingZeros().toPlainString() else "—"
                                val classified = if (scope != null && requirement != null && grades != null)
                                    UniversityCourseClassification.progress(scope, grades, courseCatalog) else null
                                val branches = classified?.categories?.singleOrNull()?.children.orEmpty()
                                val previewQuarters = if (previewSecondHalf) setOf(3, 4) else setOf(1, 2)
                                val forecast = if (preview && grades != null) SchoolCreditForecast.calculate(grades, records, previewYear, previewQuarters, profile?.faculty) else null
                                val projected = if (scope != null && requirement != null && forecast != null && grades != null)
                                    UniversityCourseClassification.forecast(scope, forecast, grades, courseCatalog) else null
                                val additional = forecast?.added ?: java.math.BigDecimal.ZERO
                                item { CampusCard {
                                    Text("GPA", fontWeight = FontWeight.SemiBold)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(grades?.cards?.firstOrNull()?.substringAfter("GPA：")?.substringBefore("（") ?: "—", fontSize = 44.sp, fontWeight = FontWeight.Bold)
                                            Text("累计 GPA", color = colors.onSurfaceVariant, fontSize = 13.sp)
                                        }
                                        Icon(Icons.Outlined.School, null, Modifier.size(72.dp), tint = Color(0xFF77B6FF))
                                    }
                                } }
                                item { Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Box(Modifier.weight(1f)) { CampusCard {
                                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(30.dp), tint = colors.primary)
                                        Text("已修学分", fontWeight = FontWeight.SemiBold)
                                        Text(earnedText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                        Text("/ $totalText", color = colors.onSurfaceVariant)
                                    } }
                                    Box(Modifier.weight(1f)) { CampusCard {
                                        Icon(Icons.Outlined.DonutLarge, null, Modifier.size(30.dp), tint = Color(0xFF36B9A7))
                                        Text("剩余学分", fontWeight = FontWeight.SemiBold)
                                        Text(remainingText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                        Text("学分", color = colors.onSurfaceVariant)
                                    } }
                                } }
                                item { CampusCard {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("毕业进度", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                        TextButton(onClick = { preview = !preview }, modifier = Modifier.widthIn(max = 190.dp)) {
                                            Text("学分预览", fontSize = 12.sp)
                                            Icon(if (preview) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
                                        }
                                    }
                                    if (preview) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(onClick = { previewYear-- }) { Text("‹") }
                                            Text("$previewYear 学年度")
                                            TextButton(onClick = { previewYear++ }) { Text("›") }
                                        }
                                        Column {
                                            FilterChip(selected = !previewSecondHalf, onClick = { previewSecondHalf = false }, label = { Text("前期 Q1+Q2") })
                                            FilterChip(selected = previewSecondHalf, onClick = { previewSecondHalf = true }, label = { Text("后期 Q3+Q4") })
                                        }
                                        if (forecast == null || forecast.courses.isEmpty()) Text("暂无课程，请同步所选学年度的课表和成绩。", fontSize = 12.sp)
                                        if (forecast != null && forecast.missing.isNotEmpty()) Text("课表尚不完整，仅预览已同步课程。", fontSize = 12.sp)
                                        Text("实色：已修得 · 虚线：预计新增", fontSize = 12.sp)
                                    }
                                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                        Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                                            ForecastRing(earned?.toFloat() ?: 0f, additional.toFloat(), requirement?.total?.toFloat() ?: 0f, Modifier.fillMaxSize())
                                            Text(if (earned != null && requirement != null) "${(ratio * 100).toInt()}%" else "—", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("$earnedText / $totalText", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                            ForecastBar(earned?.toFloat() ?: 0f, additional.toFloat(), requirement?.total?.toFloat() ?: 0f, Color(0xFF2DBDF0))
                                            if (preview && forecast != null) {
                                                Text("预计新增 +${additional.stripTrailingZeros().toPlainString()}", color = Color(0xFF9781EE), fontSize = 14.sp)
                                                Text("全部合格后 ${forecast.total.stripTrailingZeros().toPlainString()} / $totalText", fontSize = 12.sp)
                                            }
                                            Text("剩余学分", fontSize = 12.sp, color = colors.onSurfaceVariant)
                                            Text(remainingText, color = colors.onSurfaceVariant)
                                        }
                                    }
                                    if (preview) Text("预览不计入已修学分或 GPA。", fontSize = 12.sp)
                                    Text("仍需满足分类与必修要求", fontSize = 12.sp, color = colors.onSurfaceVariant)
                                } }
                                item { CampusCard {
                                    Text("分类进度", fontWeight = FontWeight.SemiBold)
                                    val palette = listOf(Color(0xFF6FAAFF), Color(0xFF49C5B3), Color(0xFFFFBD48), Color(0xFFA080F5))
                                    if (branches.isNotEmpty()) branches.forEachIndexed { index, branch ->
                                        var expanded by rememberSaveable(profile?.studentNumber, scope.toString(), branch.requirement.id) { mutableStateOf(false) }
                                        val tint = palette[index % palette.size]
                                        val predictedBranch = projected?.categories?.singleOrNull()?.children?.find { it.requirement.id == branch.requirement.id }
                                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(12.dp).background(tint, RoundedCornerShape(6.dp)))
                                            Spacer(Modifier.width(10.dp))
                                            Text(branch.requirement.label, Modifier.weight(1f), fontSize = 14.sp)
                                            Text(branch.display, fontSize = 14.sp, color = colors.onSurfaceVariant)
                                        }
                                        ForecastBar(branch.earned.toFloat(), predictedBranch?.earned?.toFloat() ?: 0f, branch.requirement.required.toFloat(), tint)
                                        if (preview) Text("预计新增 +${predictedBranch?.earned?.stripTrailingZeros()?.toPlainString() ?: "0"}", fontSize = 12.sp, color = Color(0xFF9781EE))
                                        if (expanded) Column(Modifier.fillMaxWidth().padding(start = 10.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            if (preview) ForecastBranch(branch, predictedBranch)
                                            else if (branch.children.isEmpty()) ProgressBranch(branch)
                                            else branch.children.forEach { child -> ProgressBranch(child) }
                                        }
                                        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                            Text(if (expanded) "收起明细" else "展开明细", fontSize = 12.sp)
                                            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(20.dp))
                                        }
                                    } else Text("课程分类按学校原文显示，毕业归类尚待核对。", fontSize = 13.sp)
                                    if (classified != null && classified.unassigned.signum() > 0) Text("待分类 ${classified.unassigned.stripTrailingZeros().toPlainString()} 学分", fontSize = 13.sp)
                                    if (preview && projected != null && projected.unassigned.signum() > 0) {
                                        Text("预计新增待分类 ${projected.unassigned.stripTrailingZeros().toPlainString()} 学分", fontSize = 13.sp)
                                        forecast?.courses?.filter { it.state == "预计新增" }?.forEach { c ->
                                            if (scope != null && grades != null && UniversityCourseClassification.classifyForecast(scope, c, grades, courseCatalog).let { it.categoryId == null && !it.excluded })
                                                Text("${c.name} · ${c.credits.stripTrailingZeros().toPlainString()} 学分", fontSize = 12.sp)
                                        }
                                    }
                                } }
                                item { CampusRow("学分详情与成绩", Icons.Outlined.Description) { detail = true } }
                                item { CampusRow("课程成绩", Icons.Outlined.Description) { navigate("成绩") } }
                                item { CampusRow("资料更新", Icons.Outlined.Refresh) { update() } }
                            }
                            "成绩" -> {
                                val grades = records.filter { it.grades.isNotEmpty() }.maxByOrNull { it.syncedAt }?.grades.orEmpty()
                                if (grades.isEmpty()) item { Text("暂无数据，请点击“资料更新”。") }
                                items(grades) { g -> CampusCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) { Text(g.name, fontWeight = FontWeight.SemiBold); Text("${g.year} · ${g.term}", fontSize = 12.sp, color = colors.onSurfaceVariant) }
                                        Text(g.score?.toString() ?: "—", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(14.dp))
                                        Surface(color = if(g.passed) colors.primaryContainer else colors.errorContainer, shape = RoundedCornerShape(8.dp)) {
                                            Text(g.grade, Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } }
                            }
                            "通知" -> item { CampusNotices() }
                            "常用链接" -> {
                                listOf(
                                    "教务信息门户" to "https://csweb.ibaraki.ac.jp/campusweb/portal.do?page=main",
                                    "シラバス（课程大纲）" to "https://syllabus.ibaraki.ac.jp/",
                                    "茨城大学官网" to "https://www.ibaraki.ac.jp/",
                                    "manaba" to "https://manaba.ibaraki.ac.jp/ct/home"
                                ).forEach { (title, url) -> item { CampusRow(title, Icons.Outlined.OpenInNew) {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                                } } }
                            }
                            "设置" -> {
                                item { CampusCard { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("语言", Modifier.weight(1f)); LanguageButton() } } }
                                item { CampusRow("主题设置", Icons.Outlined.Palette) { theme = true } }
                                item { CampusRow("通知设置", Icons.Outlined.Notifications) { navigate("通知设置") } }
                                item { CampusRow("退出登录", Icons.Outlined.Logout) {
                                    context.startActivity(Intent(context, IbarakiAccountActivity::class.java).putExtra("logout", true)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                                } }
                            }
                            "通知设置" -> item { CampusNotificationSettings() }
                            else -> item { CampusCard {
                                Icon(campusIcon(page), null, Modifier.size(48.dp), tint = colors.primary)
                                Text("准备中", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("此功能将逐步开放，目前不会读取或发送通知。")
                                TextButton(onClick = { navigate("首页") }) { Text("返回首页") }
                            } }
                        }
                        item { SupportFooter(Modifier.fillMaxWidth().padding(top = 8.dp)) }
                    }
                }
            }
        }
        if (theme) AppThemeSettingsDialog { theme = false }
        if (syncInfo) AlertDialog(onDismissRequest = { syncInfo = false }, title = { Text("同步状态") }, text = { androidx.compose.foundation.lazy.LazyColumn { item { Text(syncMessage) }; if (syncDetail.isNotBlank()) item { Text(syncDetail) } } }, confirmButton = { TextButton(onClick = { syncInfo = false }) { Text("关闭") } })
    }
}

@Composable
fun CampusIllustration(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.campus_banner), contentDescription = null,
        modifier = modifier, contentScale = ContentScale.Crop, alignment = Alignment.BottomCenter)
}

@Composable
private fun CampusCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun CampusRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp))
            Text(title, Modifier.weight(1f)); Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
        }
    }
}

@Composable
private fun CampusCourseList(records: List<PortalImport>) {
    val today = rememberPhoneDate()
    val currentYear = today.take(4).toInt() - if (today.substring(5, 7).toInt() < 4) 1 else 0
    var year by rememberSaveable { mutableIntStateOf(currentYear) }
    var quarter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var description by remember { mutableStateOf<String?>(null) }
    var syllabusTerm by remember { mutableStateOf("") }
    val tables = records.filter { it.key.substringAfterLast('/').startsWith("timetable-$year-Q") }
    val rows = tables.filter { quarter == 0 || it.key.endsWith("Q$quarter") }
        .flatMap { table -> table.lessons.map { table.key.substringAfterLast('/').removePrefix("timetable-") to it } }
        .filter { it.second.description.contains(query, ignoreCase = true) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = { year-- }) { Text("‹") }
            Text("$year 学年度", fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { year++ }) { Text("›") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..4).forEach { q -> FilterChip(selected = quarter == q, onClick = { quarter = q }, label = { Text(if (q == 0) "全部" else "Q$q", fontSize = 12.sp) }) }
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("搜索课程") }, singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(20.dp))
        if (rows.isEmpty()) CampusCard {
            Text("$year 学年度")
            Text(if (tables.isEmpty()) "所选学年度尚无已同步课表。" else "暂无课程")
        }
        rows.forEach { (term, lesson) ->
            Card(onClick = { syllabusTerm = term; description = lesson.description }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(lesson.description.substringAfter(' ').substringBeforeLast(' ').substringBefore("【"), fontWeight = FontWeight.SemiBold)
                        Text("$term · ${listOf("", "月", "火", "水", "木", "金", "土", "日").getOrElse(lesson.day) { "" }} ${lesson.period}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                }
            }
        }
    }
    description?.let { value -> CourseSyllabusDialog(syllabusTerm, value) { description = null } }
}

private fun campusTitle(page: String) = when(page) { "课程" -> "履修与课程"; "学分" -> "学分与进度"; else -> page }
private fun campusIcon(page: String): ImageVector = when(page) {
    "首页" -> Icons.Outlined.Home
    "时间表" -> Icons.Outlined.CalendarMonth
    "课程" -> Icons.AutoMirrored.Outlined.MenuBook
    "学分" -> Icons.Outlined.BarChart
    "通知" -> Icons.Outlined.Description
    "常用链接" -> Icons.Outlined.Link
    else -> Icons.Outlined.Settings
}
private fun Color.luminanceForCampus() = (red * .2126f + green * .7152f + blue * .0722f)

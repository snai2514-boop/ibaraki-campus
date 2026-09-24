package com.tyust.course.ui.screen

import com.tyust.course.i18n.LocalizedText as Text
import androidx.compose.material3.Text as RawText


import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.*
import com.tyust.course.schedule.ScheduleIdentity
import com.tyust.course.ui.system.GlassPageScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun SchoolTimetableScreen(onBack: () -> Unit, home: Boolean = false) {
    val context = LocalContext.current
    val profile by SchoolSyncState.profile.collectAsState()
    val syncing by SchoolSyncState.running.collectAsState()
    val dataRevision by SchoolSyncState.dataRevision.collectAsState()
    val deliveryRevision by SchoolDeliveryStore.revision.collectAsState()
    val syncMessage by SchoolSyncState.message.collectAsState()
    val syncDetail by SchoolSyncState.details.collectAsState()
    val currentPhoneDate = rememberPhoneDate()
    var syncDetails by remember { mutableStateOf(false) }
    var snapshots by remember { mutableStateOf(SchoolSyncState.snapshots.value.filter { it.key.substringAfterLast('/').startsWith("timetable-") && UniversityCurricula.profileMatches(profile, it.key) }) }
    var selected by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var month by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var termMenu by remember { mutableStateOf(false) }
    var yearMenu by remember { mutableStateOf(false) }
    var weekMenu by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<String?>(null) }
    var showCalendarIssues by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    fun phoneToday() = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
    var todaySeen by remember { mutableStateOf(phoneToday()) }
    var followToday by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var personal by remember { mutableStateOf<List<PersonalCalendarEntry>>(emptyList()) }
    var personalReady by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PersonalCalendarEntry?>(null) }
    var adding by remember { mutableStateOf(false) }
    val personalStore = remember(context) { PersonalCalendarStore(context) }
    val classroomStore = remember(context) { SchoolClassroomStore(context) }
    var classrooms by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var liveMeetings by remember { mutableStateOf<List<SchoolLiveMeeting>>(emptyList()) }
    var schoolDescription by remember { mutableStateOf<String?>(null) }
    var schoolVenue by remember { mutableStateOf("") }
    var roomText by remember { mutableStateOf("") }
    var roomError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context) {
        val lifecycle = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) { revision++; val now = phoneToday(); if(now != todaySeen) { followToday = true; todaySeen = now } } }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(revision, dataRevision, profile?.studentNumber) {
        loading = true
        try {
            personal = withContext(Dispatchers.IO) { personalStore.load() }
            personalReady = true
            classrooms = withContext(Dispatchers.IO) { classroomStore.load() }
            liveMeetings = withContext(Dispatchers.IO) { profile?.let { SchoolLiveCalendarStore(context, UniversityCurricula.owner(it)).load() }.orEmpty() }
            snapshots = withContext(Dispatchers.IO) { SchoolImportStore(context).load().filter { it.key.substringAfterLast('/').startsWith("timetable-") && profile?.let { p -> UniversityCurricula.profileMatches(p, it.key) } == true } }
            if (selected.isNotEmpty() && profile?.let { UniversityCurricula.profileMatches(it, selected) } != true) selected = ""
            if (selected.isEmpty()) {
                val today = SchoolTermView.format(Calendar.getInstance(IbarakiTimetable.zone))
                selected = snapshots.filter { SchoolAcademicCalendar.resolve(it, profile).events.firstOrNull()?.date?.let { date -> date <= today } == true }
                    .maxByOrNull { it.key.substringAfterLast('/') }?.key ?: snapshots.firstOrNull()?.key ?: "timetable-2026-Q1"
            }
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { error = "学校课表读取失败，已保留原记录。" }
        finally { loading = false }
    }
    val academicYear = Regex("timetable-(\\d{4})-Q").find(selected)?.groupValues?.get(1)?.toIntOrNull() ?: 2026
    val admissionYear = profile?.admissionYear ?: profile?.curriculumYear ?: academicYear
    val studyYear = academicYear - admissionYear + 1
    val ownerPrefix = profile?.let { UniversityCurricula.owner(it) + "/" } ?: selected.substringBeforeLast('/', "").let { if(it.isBlank()) "" else "$it/" }
    val item = snapshots.find { it.key == selected }
    val quarter = selected.substringAfterLast('Q').toIntOrNull()?.coerceIn(1, 4) ?: 1
    val calendarResult = remember(item, profile) { item?.let { SchoolAcademicCalendar.resolve(it, profile) } ?: SchoolAcademicCalendar.Resolution(emptyList(), emptyList()) }
    // A displayed week/month may straddle terms. Resolve every saved term for this student.
    val sourcedEvents = remember(snapshots,profile,liveMeetings) { SchoolCalendarNavigation.merge(
        SchoolLiveCalendar.sources(profile?.let { UniversityCurricula.owner(it) }.orEmpty(), snapshots, liveMeetings) + snapshots.sortedByDescending {it.syncedAt}.flatMap { snapshot ->
        SchoolAcademicCalendar.resolve(snapshot,profile).events.map {SchoolCalendarNavigation.Source(snapshot.key,it)}
    }) }
    val events = remember(sourcedEvents) {sourcedEvents.map {it.event}}
    val eventSources = remember(sourcedEvents) {sourcedEvents.associate {it.event to it.key}}
    val deliveryStore = remember(profile?.studentNumber) { profile?.let { SchoolDeliveryStore(context,UniversityCurricula.owner(it)) } }
    val deliveries = remember(sourcedEvents, deliveryRevision, deliveryStore) { sourcedEvents.associate { source ->
        val code=source.event.lesson.description.substringBefore(' ')
        val year=Regex("timetable-(\\d{4})-").find(source.key)?.groupValues?.get(1)?.toIntOrNull() ?: academicYear
        "${source.key}/$code" to deliveryStore?.load(year,code).orEmpty()
    } }
    val termModel = remember(selected, events, classrooms, deliveries) { SchoolTermView(quarter, events, academicYear) { event ->
        val code = event.lesson.description.substringBefore(' ')
        val source=eventSources[event] ?: selected
        val room = classrooms[SchoolClassroomMatch.key(source,code,event.date,event.lesson.period)]
            ?: classrooms["$source/$code"].orEmpty()
        SchoolCourseVenue.label(source,event.lesson.description,room,deliveries["$source/$code"].orEmpty())
    } }
    val outsideTerm = date.isNotBlank() && date !in termModel.date(1, 1)..termModel.date(termModel.weekCount, 7)
    val model = remember(termModel, outsideTerm, date) {
        if (!outsideTerm) termModel else SchoolTermView(quarter, events, academicYear,
            SchoolTermView.format(IbarakiTimetable.parseDate(date)!!.apply { add(Calendar.DAY_OF_MONTH, -((get(Calendar.DAY_OF_WEEK) + 5) % 7)) }), termModel.venue)
    }
    val week=if(date.isBlank() || outsideTerm) 1 else termModel.week(date).coerceIn(1,termModel.weekCount)
    fun changeDate(next: String) {
        date=next; month=next.take(7)+"-01"
        selected=SchoolCalendarNavigation.termKey(next,ownerPrefix)
        followToday=false
    }
    fun selectTerm(key: String) {
        selected=key
        val year=Regex("timetable-(\\d{4})-").find(key)!!.groupValues[1].toInt()
        val target=SchoolTermView(key.last().digitToInt(),emptyList(),year)
        date=target.firstMonday; month=date.take(7)+"-01"; followToday=false
    }
    val terms = (1..4).map { "${ownerPrefix}timetable-$academicYear-Q$it" }
    val owners = snapshots.map { it.key.substringBefore('/') }.distinct()
    fun label(key: String) = key.substringAfterLast('/').removePrefix("timetable-").replace("-Q", " · Q") +
        if (owners.size > 1) " · ${key.take(6)}" else ""
    LaunchedEffect(loading, todaySeen, profile?.studentNumber) {
        if(!loading) changeDate(if(followToday || date.isBlank()) phoneToday() else date)
    }
    val palette = listOf(Color(0xFF516BB0), Color(0xFF398776), Color(0xFF9E6651), Color(0xFF8660A5))
    fun sourceKey(description: String) = sourcedEvents.firstOrNull {it.event.lesson.description==description && it.event.date in model.date(week,1)..model.date(week,7)}?.key ?: selected
    fun roomKey(description: String) = "${sourceKey(description)}/${description.substringBefore(' ')}"
    val schoolCourses = remember(model, classrooms) { model.courses.map { row ->
        val id = "$selected/${row.description}/${row.day}/${row.period}/${row.location}"
        ScheduleCourseUi(row.description.substringAfter(' ').substringBeforeLast(' ').substringBefore("【"), "", row.location, row.day, row.period, row.period,
            row.weeks, palette[ScheduleIdentity.colorIndex(row.description.substringBefore(' '), palette.size)], id = id)
    } }
    val personalCourses = remember(personal, model) { personal.filter { it.kind == "课程" && !it.allDay }.flatMap { entry ->
        (1..model.weekCount).flatMap { w -> (1..7).mapNotNull { day ->
            if (!entry.occursOn(model.date(w, day))) return@mapNotNull null
            val periods = IbarakiTimetable.periodTimes.filter { (_, times) -> entry.start < times.second && entry.end > times.first }.keys
            if (periods.isEmpty()) return@mapNotNull null
            ScheduleCourseUi(entry.title, "", entry.location, day, periods.min(), periods.max(), "$w", Color(0xFF987040),
                isCustom = true, customId = entry.id, id = "personal:${entry.id}:$w:$day")
        } }
    } }
    val courses = schoolCourses + personalCourses
    fun editPersonal(id: String) { editing = personal.find { id.startsWith("personal:${it.id}:") }; adding = false }
    val weekEntries = personal.flatMap { e -> (1..7).mapNotNull { day -> model.date(week, day).takeIf(e::occursOn)?.let { it to e } } }
    fun changeWeek(value: Int) {
        changeDate(SchoolCalendarNavigation.moveWeek(date.ifBlank {model.date(week,1)},value-week))
    }
    fun export() {
        if (item == null || calendarResult.issues.isNotEmpty()) return
        runCatching {
            val file = java.io.File(context.cacheDir, "ibaraki-${item.key.substringAfterLast('/')}.ics")
            file.writeText(SchoolQ2Calendar.ics(item, profile))
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "导出学季日历"))
        }.onFailure { error = "导出失败，请重试。" }
    }
    GlassPageScaffold(title = if (home) "茨城大学" else "日历", subtitle = if (home) "日历" else "茨城大学 · 学校同步", onBack = if (home) null else onBack,
        actions = { if (home) {
            TextButton(onClick = {
                context.startActivity(Intent(context, com.tyust.course.IbarakiAccountActivity::class.java)
                    .putExtra("logout", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }) { Text("退出登录") }
            com.tyust.course.i18n.LanguageButton()
        } },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(modifier = Modifier.weight(1f), enabled = personalReady && date.isNotBlank(), onClick = { adding = true; editing = PersonalCalendarEntry(title = "", kind = "课程", date = date, endDate = maxOf(date, model.date(model.weekCount, 7))) }) { Text("+ 课程") }
                    Button(modifier = Modifier.weight(1f), enabled = personalReady && date.isNotBlank(), onClick = { adding = true; editing = PersonalCalendarEntry(title = "", kind = "事件", date = date, allDay = true) }) { Text("+ 事件") }
                }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (home) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        profile?.let { p ->
                            RawText(p.name, style = MaterialTheme.typography.titleMedium)
                            Text(listOf(p.faculty, p.department, p.year).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            Text("学号：${p.studentNumber}", style = MaterialTheme.typography.bodySmall)
                        } ?: Text(if (syncing) "后台同步学生资料中，可正常使用" else "尚未获取学生资料，请点击更新资料", style = MaterialTheme.typography.bodySmall)
                    }
                    Column {
                        OutlinedButton(onClick = { context.startActivity(Intent(context, com.tyust.course.StudyProgressActivity::class.java)) }) { Text("GPA / 学分") }
                        TextButton(enabled = !syncing, onClick = { context.startActivity(Intent(context, com.tyust.course.IbarakiPortalActivity::class.java).putExtra("backgroundSync", true)) }) { Text("更新资料") }
                    }
                }
                if (syncMessage.isNotBlank()) Text(
                    if (syncing) "后台同步中，可正常使用 · 查看详情" else if (syncMessage.startsWith("同步完成")) "今日学校数据已更新 · 查看详情" else "更新提示 · 点击查看",
                    Modifier.padding(horizontal = 16.dp).clickable { syncDetails = true }, style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    val current = IbarakiTimetable.parseDate(date) ?: Calendar.getInstance(IbarakiTimetable.zone)
                    android.app.DatePickerDialog(context, { _, y, m, d ->
                        changeDate("%04d-%02d-%02d".format(java.util.Locale.ROOT, y, m + 1, d))
                    }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
                }) { Text((date.ifBlank { "选择日期" }) + " ▾", style = MaterialTheme.typography.titleLarge) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(onClick = { yearMenu = true }) { Text(if(studyYear in 1..4) listOf("大一", "大二", "大三", "大四")[studyYear - 1] + " ▾" else "$academicYear ▾") }
                        DropdownMenu(yearMenu, { yearMenu = false }) {
                            (1..4).forEach { year -> DropdownMenuItem(text = { Row { Text(listOf("大一", "大二", "大三", "大四")[year - 1]); RawText(" · ${admissionYear + year - 1}") } }, onClick = {
                                selectTerm("${ownerPrefix}timetable-${admissionYear + year - 1}-Q$quarter"); yearMenu = false
                            }) }
                        }
                    }
                    Box {
                        TextButton(onClick = { termMenu = true }) { Text(if (selected.isEmpty()) "选择学季" else label(selected) + " ▾") }
                        DropdownMenu(termMenu, { termMenu = false }) { terms.forEach { term ->
                            DropdownMenuItem(text = { Text(label(term)) }, onClick = { selectTerm(term); termMenu = false })
                        } }
                    }
                    if (tab == 0 && !outsideTerm) TextButton(onClick = { weekMenu = true }) { Text("第 $week 周 ▾") }
                }
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1 to "月", 0 to "周").forEach { (value, title) ->
                        Button(onClick = { tab = value }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (tab == value) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (tab == value) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant),
                            elevation = null) { Text(title) }
                    }
                }
            }
            SchoolHolidays.classroomNotice(currentPhoneDate)?.let { Text(it) }
            SchoolHolidays.on(date)?.let { holiday ->
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("假期", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Text(holiday.name, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        RawText(holiday.start.substring(5) + " – " + holiday.end.substring(5), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error) }
            if (item == null && !loading) Text("此学季尚无学校课表，点击同步更新。", Modifier.padding(16.dp))
            if (calendarResult.issues.isNotEmpty()) {
                TextButton(onClick = { showCalendarIssues = true }) { Text("${calendarResult.issues.size} 条课程日期待核对 · 查看明细") }
            }
            if (showCalendarIssues) AlertDialog(onDismissRequest = { showCalendarIssues = false }, title = { Text("课程日期待核对") }, text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    Text("其他已确认课程正常显示。以下课程保留学校原始星期与节次，不猜测授课日期。")
                    calendarResult.issues.forEach { issue ->
                        RawText("${issue.lesson.description}\n星期 ${issue.lesson.day} · 第 ${issue.lesson.period} 限\n${issue.reason}", Modifier.padding(vertical = 8.dp))
                    }
                }
            }, confirmButton = { TextButton(onClick = { showCalendarIssues = false }) { Text("关闭") } })
            if (academicYear != 2026) Text("完整学年度校历尚待核对；已公布的学校日程按实际日期显示，未公布的日期不推算。", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
            run {
            when (tab) {
                0 -> key(selected) { Column(Modifier.weight(1f)) {
                    if (weekEntries.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth().heightIn(max = 130.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                        items(weekEntries) { (day, e) -> TextButton(onClick = { adding = false; editing = e }) {
                            Text("${day.substring(5)} · ${if(e.allDay) "全天" else "${e.start}–${e.end}"} · ${e.title}${if(e.location.isBlank()) "" else " · ${e.location}"}")
                        } }
                    }
                    Box(Modifier.weight(1f)) {
                    CampusWeekGrid(week, courses, model, onWeekChange = ::changeWeek, calendarBrowsing = true,
                        onCourseClick = { course -> if (course.id.startsWith("personal:")) editPersonal(course.id) else {
                            val row = model.courses.find { "$selected/${it.description}/${it.day}/${it.period}/${it.location}" == course.id }
                            schoolDescription = row?.description
                            schoolVenue = row?.location.orEmpty()
                            roomText = row?.let { classrooms[roomKey(it.description)] }.orEmpty(); roomError = null
                        } },
                        onExportClick = ::export)
                } } }
                1 -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { SchoolMonthGrid(month, date, events, model, personal, onMonth = ::changeDate, onDate = ::changeDate) }
                    item { Text(date + if(!outsideTerm && date in model.date(1, 1)..model.date(model.weekCount, 7)) " · 第 ${model.week(date)} 周" else "", style = MaterialTheme.typography.titleMedium) }
                    SchoolReserveDays.on(date)?.let { label ->
                        item { Text(label, color = MaterialTheme.colorScheme.tertiary); Text("是否上课或考试，以教师通知为准。", style = MaterialTheme.typography.bodySmall) }
                    }
                    item { TextButton(onClick = { tab = 0 }, enabled = date.isNotBlank()) { Text("查看这一周的课表") } }
                    val daily = events.filter { it.date == date }
                    val localDaily = personal.filter { it.occursOn(date) }.sortedBy { if(it.allDay) "" else it.start }
                    if (daily.isEmpty() && localDaily.isEmpty()) item { Text("当天没有安排") }
                    items(localDaily, key = { "personal:${it.id}" }) { e -> Card(Modifier.fillMaxWidth().clickable { adding = false; editing = e }) { Column(Modifier.padding(12.dp)) {
                        Text("${e.kind} · ${if (e.allDay) "全天" else "${e.start}–${e.end}"}")
                        RawText(e.title)
                        if(e.location.isNotBlank()) RawText(e.location)
                        if(e.notes.isNotBlank()) RawText(e.notes)
                    } } }
                    items(daily) { event -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                        Text("第 ${event.lesson.period} 限 · ${IbarakiTimetable.periodTimes[event.lesson.period]?.first}")
                        RawText(event.lesson.description)
                        Text(model.venue(event))
                        if (event.examPossible) Text("考试可能日，以教师通知为准", style = MaterialTheme.typography.bodySmall)
                    } } }
                }
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("本学季 ${item?.lessons?.size ?: 0} 条课程安排 · ${events.size} 次日历计划") }
                    item { Text("周次按日历周计算。日期依据学校校历，教师临时调课尚未同步。", style = MaterialTheme.typography.bodySmall) }
                    items(item?.lessons.orEmpty()) { lesson -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                        Text("周${"一二三四五六日"[lesson.day - 1]} · 第 ${lesson.period} 限")
                        RawText(lesson.description)
                    } } }
                    item { TextButton(onClick = ::export, enabled = item != null && calendarResult.issues.isEmpty()) { Text("导出本学季日历") } }
                }
            }
            }
        }
    }
    if (syncDetails) AlertDialog(onDismissRequest = { syncDetails = false }, title = { Text("资料更新") },
        text = { LazyColumn { item { Text(syncMessage) }; if (syncDetail.isNotBlank()) item { Text(syncDetail) } } }, confirmButton = { TextButton(onClick = { syncDetails = false }) { Text("关闭") } })
    if (weekMenu) AlertDialog(onDismissRequest = { weekMenu = false }, title = { Text("选择具体周次") }, text = {
        LazyColumn { items((1..model.weekCount).toList()) { n ->
            TextButton(onClick = { changeWeek(n); weekMenu = false }) { Text("第 $n 周  ${model.date(n, 1).substring(5)} – ${model.date(n, 7).substring(5)}") }
        } }
    }, confirmButton = { TextButton(onClick = { weekMenu = false }) { Text("关闭") } })
    detail?.let { AlertDialog(onDismissRequest = { detail = null }, title = { Text("学校课程") }, text = { Text(it) },
        confirmButton = { TextButton(onClick = { detail = null }) { Text("关闭") } }) }
    schoolDescription?.let { description -> AlertDialog(onDismissRequest = { schoolDescription = null }, title = { Text("学校课程") }, text = {
        Column {
            RawText(description)
            if (schoolVenue.isNotBlank()) RawText(schoolVenue, Modifier.padding(top = 12.dp))
            val source=sourceKey(description)
            val delivery=deliveries["$source/${description.substringBefore(' ')}"].orEmpty()
            val venue=SchoolCourseVenue.label(source, description,roomText,delivery)
            Text(if(delivery.isNotBlank()) "学校大纲：$delivery" else SchoolCourseVenue.explanation(source, description), Modifier.padding(vertical = 12.dp))
            if (!venue.startsWith("线上")) {
                OutlinedTextField(roomText, { roomText = it }, label = { Text("补充已确认的教室") })
                Text("学校未提供教室时，可在此补充；仅保存到本机。", style = MaterialTheme.typography.bodySmall)
            } else Text(venue)
            roomError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        runCatching { classrooms = classroomStore.put(roomKey(description), roomText); schoolDescription = null }
            .onFailure { roomError = "保存失败，原记录已保留" }
    }) { Text("确定") } }, dismissButton = { TextButton(onClick = { schoolDescription = null }) { Text("关闭") } }) }
    editing?.let { entry -> key(entry.id) { PersonalCalendarEditor(entry, onClose = { editing = null }, onSave = { value ->
        runCatching { personal = personalStore.put(value); null }.getOrElse { "保存失败，原记录已保留" }
    }, onDelete = if (adding) null else {{ runCatching { personal = personalStore.delete(entry.id); null }.getOrElse { "删除失败，原记录已保留" } }}) } }
}

@Composable
private fun SchoolMonthGrid(month: String, selectedDate: String, events: List<SchoolCalendarEvent>, model: SchoolTermView,
    personal: List<PersonalCalendarEntry>,
    onMonth: (String) -> Unit, onDate: (String) -> Unit) {
    val first = IbarakiTimetable.parseDate(month) ?: return
    val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val counts = events.groupingBy { it.date }.eachCount()
    fun shift(amount: Int) = SchoolTermView.format((first.clone() as Calendar).apply { add(Calendar.MONTH, amount) })
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { onMonth(shift(-1)) }) { Text("上月") }
            Text(month.take(7), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { onMonth(shift(1)) }) { Text("下月") }
        }
        Row { "一二三四五六日".forEach { Text(it.toString(), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
        for (row in 0 until (offset + days + 6) / 7) Row(Modifier.fillMaxWidth()) {
            for (column in 0..6) {
                val day = row * 7 + column - offset + 1
                val value = month.take(8) + day.toString().padStart(2, '0')
                val enabled = day in 1..days
                Surface(Modifier.weight(1f).height(68.dp).padding(2.dp).clickable(enabled = enabled) { onDate(value) },
                    shape = MaterialTheme.shapes.small,
                    color = if (value == selectedDate) MaterialTheme.colorScheme.primaryContainer else if(enabled && SchoolHolidays.on(value) != null) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .45f) else Color.Transparent) {
                    if (day in 1..days) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(day.toString(), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .35f))
                        SchoolHolidays.on(value)?.let { Text(it.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }
                        if (SchoolReserveDays.on(value) != null) Text("预备日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        val count = (counts[value] ?: 0) + personal.count { it.occursOn(value) }
                        Text(if (count > 0) "$count 项" else "", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

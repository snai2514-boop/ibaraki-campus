package com.tyust.course.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyust.course.academic.IbarakiTimetable
import com.tyust.course.academic.SchoolTermView
import com.tyust.course.schedule.ScheduleIdentity
import com.tyust.course.i18n.LocalizedText as Text

/** Display-only abbreviations; the original venue remains available in course details. */
private fun compactCalendarVenue(location: String): String = location
    .replace(Regex("共通教育棟([0-9０-９]+)号館\\s*([0-9０-９]+)番教室"), "共通$1号馆\n$2教室")
    .replace("线上授课（实时）", "线上 · 实时")
    .replace("线上授课（录播）", "线上 · 录播")
    .replace("水戸:第1アリーナ(大体育館)", "水戸\n第1体育馆")

/** The school calendar uses a flat pastel grid; each overlapping course remains clickable. */
@Composable
internal fun CampusWeekGrid(week: Int, courses: List<ScheduleCourseUi>, model: SchoolTermView,
    onWeekChange: (Int) -> Unit, calendarBrowsing: Boolean = false, onCourseClick: (ScheduleCourseUi) -> Unit, onExportClick: () -> Unit) {
    val visible = courses.filter { com.tyust.course.schedule.ScheduleWeeks.parse(it.weeks).let { parsed -> parsed.valid && week in parsed.weeks } }
    var weekends by remember { mutableStateOf(false) }
    val days = if (weekends || visible.any { it.day > 5 } || (6..7).any {
        com.tyust.course.academic.SchoolReserveDays.on(model.date(week, it)) != null
    }) 7 else 5
    val dark = MaterialTheme.colorScheme.background.let { it.red + it.green + it.blue < 1.5f }
    val line = if (dark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFEBF2FA)
    val header = if (dark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF0F6FD)
    val pastels = listOf(Color(0xFFBFE9FA), Color(0xFFFFD5D9), Color(0xFFC9F2E4), Color(0xFFFFEDB9), Color(0xFFE1D7FC), Color(0xFFD9EFFF))
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onWeekChange(week - 1) }, enabled = calendarBrowsing || week > 1) { Text("‹", fontSize = 24.sp) }
                Text(model.date(week, 1).substring(5).replace('-', '/') + " – " + model.date(week, days).substring(5).replace('-', '/'),
                    Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                TextButton(onClick = { onWeekChange(week + 1) }, enabled = calendarBrowsing || week < model.weekCount) { Text("›", fontSize = 24.sp) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { weekends = !weekends }) { Text(if (weekends) "隐藏周末" else "显示周末", fontSize = 11.sp) }
            TextButton(onClick = onExportClick) { Text("导出", fontSize = 11.sp) }
        }
        Row(Modifier.fillMaxWidth().background(header, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
            Spacer(Modifier.width(40.dp))
            (1..days).forEach { day -> Column(Modifier.weight(1f).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("一二三四五六日"[day - 1].toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(model.date(week, day).substring(5).replace('-', '/'), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.tyust.course.academic.SchoolHolidays.on(model.date(week, day))?.let {
                    Text(it.name, fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                }
                if (com.tyust.course.academic.SchoolReserveDays.on(model.date(week, day)) != null) {
                    Text("预备日", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                }
            } }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            IbarakiTimetable.periodTimes.forEach { (period, time) ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Column(Modifier.width(40.dp).heightIn(min = 92.dp).padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(period.toString(), fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text(time.first, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(time.second, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    (1..days).forEach { day ->
                        val entries = visible.filter { it.day == day && period in it.startPeriod..it.endPeriod }
                        Column(Modifier.weight(1f).fillMaxHeight().heightIn(min = 92.dp).background(MaterialTheme.colorScheme.surface).border(.5.dp, line).padding(2.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            entries.forEach { course ->
                                val pastel = pastels[ScheduleIdentity.colorIndex(course.name, pastels.size)]
                                Surface(onClick = { onCourseClick(course) }, modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
                                    shape = RoundedCornerShape(7.dp), color = if(dark) pastel.copy(alpha = .23f) else pastel) {
                                    Column(Modifier.padding(horizontal = 4.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text(course.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                        if(course.location.isNotBlank()) Text(compactCalendarVenue(course.location), Modifier.padding(top = 6.dp), fontSize = 10.sp,
                                            lineHeight = 13.sp, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

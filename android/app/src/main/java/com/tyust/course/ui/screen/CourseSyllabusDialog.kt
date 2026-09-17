package com.tyust.course.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.*
import com.tyust.course.IbarakiPortalActivity
import com.tyust.course.i18n.LocalizedText as Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
internal fun CourseSyllabusDialog(term: String, description: String, onClose: () -> Unit) {
    val context=LocalContext.current
    val profile by SchoolSyncState.profile.collectAsState()
    val running by SchoolSyncState.running.collectAsState()
    val message by SchoolSyncState.message.collectAsState()
    val year=term.substringBefore("-Q").toIntOrNull() ?: 0
    val quarter=term.substringAfterLast('Q').toIntOrNull() ?: 0
    val code=description.substringBefore(' ')
    val owner=profile?.let(UniversityCurricula::owner).orEmpty()
    var data by remember(term,code,owner) { mutableStateOf<JSONObject?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    fun update() {
        context.startActivity(Intent(context,IbarakiPortalActivity::class.java).putExtra("backgroundSync",true)
            .putExtra("noticesOnly",true).putExtra("syllabusCode",code).putExtra("syllabusYear",year).putExtra("syllabusQuarter",quarter))
    }
    LaunchedEffect(owner,term,code,running) {
        data=withContext(Dispatchers.IO) { SchoolSyllabusStore(context,owner).load(year,code) }
        loaded=true
    }
    LaunchedEffect(loaded) { if(loaded && data==null && owner.isNotEmpty() && !running) update() }
    AlertDialog(onDismissRequest=onClose, title={ Text("课程大纲") }, text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(description)
            if(running) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在读取课程大纲…") }
            data?.let { d ->
                val rows=d.optJSONArray("rows")
                Text("授课计划：${rows?.length() ?: 0} 次")
                if(d.optString("delivery").isNotBlank()) { Text("授课方式"); Text(d.optString("delivery")) }
                Text("期末考试")
                Text(SyllabusSummary.exam(d.optString("grading"), (0 until (rows?.length() ?: 0)).joinToString("\n") { rows!!.getJSONObject(it).optString("subject") }))
                Text("评分方法")
                Text(d.optString("grading").ifBlank { "学校未注明" })
                Text("教科书")
                Text(d.optString("textbook").ifBlank { "学校未注明" })
                if(d.optString("notes").isNotBlank()) { Text("修课注意事项"); Text(d.optString("notes")) }
                TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "收起授课计划" else "展开授课计划") }
                if(expanded) (0 until (rows?.length() ?: 0)).forEach { i ->
                    val row=rows!!.getJSONObject(i)
                    HorizontalDivider()
                    Text(row.optString("when")+" · "+row.optString("subject"))
                    Text(row.optString("content"))
                    if(row.optString("notes").isNotBlank()) Text(row.optString("notes"))
                }
            } ?: run { if(loaded && !running) Text(message.ifBlank { "尚未读取课程大纲" }) }
        }
    }, confirmButton={ TextButton(onClick=onClose) { Text("关闭") } }, dismissButton={ TextButton(enabled=!running && owner.isNotEmpty(),onClick=::update) { Text("资料更新") } })
}

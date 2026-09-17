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
import com.tyust.course.IbarakiPortalActivity
import com.tyust.course.academic.*
import com.tyust.course.i18n.LocalizedText as Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CampusNotices() {
    val context = LocalContext.current
    val profile by SchoolSyncState.profile.collectAsState()
    val syncing by SchoolSyncState.running.collectAsState()
    val message by SchoolSyncState.message.collectAsState()
    val owner = profile?.let(UniversityCurricula::owner)
    var data by remember(owner) { mutableStateOf(SchoolNotices()) }
    var selected by remember(owner) { mutableStateOf<String?>(null) }
    var changes by remember(owner) { mutableStateOf<List<String>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(owner, syncing) {
        if(owner != null) changes = AcademicChangeInbox(context, owner).rows()
        if(owner != null) runCatching { withContext(Dispatchers.IO) { SchoolNoticeStore(context, owner).load() } }
            .onSuccess { data = it; failed = false }.onFailure { failed = true }
    }
    fun update(id: String = "") {
        context.startActivity(Intent(context, IbarakiPortalActivity::class.java).putExtra("backgroundSync", true)
            .putExtra("noticesOnly",true).putExtra("noticeId",id))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("学业资料变化", style = MaterialTheme.typography.titleLarge)
        if (changes.isEmpty()) Text("暂无学分或 GPA 变化。")
        changes.forEach { change -> Card(Modifier.fillMaxWidth()) { Text(change, Modifier.padding(16.dp)) } }
        Text("履修・成績", style = MaterialTheme.typography.titleLarge)
        Text("学校公告", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { update() }, enabled = !syncing && owner != null) { Text(if(syncing) "正在同步…" else "更新资料") }
        if(syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
        if(failed) Text("公告读取失败，已保留本机记录。")
        if(data.syncedAt > 0) Text(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",java.util.Locale.ROOT).format(java.util.Date(data.syncedAt)), style = MaterialTheme.typography.bodySmall)
        if(data.rows.isEmpty()) Text("暂无公告，请更新资料。")
        else Text("${data.rows.size} 条", style = MaterialTheme.typography.labelMedium)
        if(data.syncedAt > 0 && !data.complete) Text("公告列表尚未完整读取，已保留旧记录。")
        data.rows.forEach { notice ->
            Card(onClick = { selected = notice.id; if(notice.body.isBlank() && !syncing) update(notice.id) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Text(notice.title, style = MaterialTheme.typography.titleSmall)
                    Text(notice.published, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    selected?.let { id -> data.rows.find { it.id == id }?.let { notice ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("学校公告") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                androidx.compose.material3.Text(notice.body.ifBlank { notice.title + "\n" + notice.period })
                if(notice.body.isBlank()) Text(if(syncing) "正在读取正文…" else "正文尚未读取，请重试。")
                Text("附件请在学校网页查看。", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(onClick = { selected = null }) { Text("关闭") } },
            dismissButton = { TextButton(onClick = { update(id) }, enabled = !syncing) { Text("更新资料") } })
    } }
}

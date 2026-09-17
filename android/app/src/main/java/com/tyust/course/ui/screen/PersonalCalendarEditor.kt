package com.tyust.course.ui.screen

import com.tyust.course.i18n.LocalizedText as Text
import androidx.compose.material3.Text as RawText


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.PersonalCalendarEntry

@Composable
fun PersonalCalendarEditor(initial: PersonalCalendarEntry, onClose: () -> Unit,
    onSave: (PersonalCalendarEntry) -> String?, onDelete: (() -> String?)? = null) {
    var title by remember { mutableStateOf(initial.title) }
    var date by remember { mutableStateOf(initial.date) }
    var endDate by remember { mutableStateOf(initial.endDate) }
    var start by remember { mutableStateOf(initial.start) }
    var end by remember { mutableStateOf(initial.end) }
    var allDay by remember { mutableStateOf(initial.allDay) }
    var weekly by remember { mutableStateOf(initial.weekly) }
    var location by remember { mutableStateOf(initial.location) }
    var notes by remember { mutableStateOf(initial.notes) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, title = { Text("${if (onDelete == null) "添加" else "编辑"}${initial.kind}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("名称") }, singleLine = true)
            OutlinedTextField(date, { date = it }, label = { Text("日期 YYYY-MM-DD") }, singleLine = true)
            Row { Checkbox(allDay, { allDay = it }); Text("全天", Modifier.padding(top = 12.dp)) }
            if (!allDay) {
                OutlinedTextField(start, { start = it }, label = { Text("开始时间 HH:mm") }, singleLine = true)
                OutlinedTextField(end, { end = it }, label = { Text("结束时间 HH:mm") }, singleLine = true)
            }
            Row { Checkbox(weekly, { weekly = it }); Text("每周同一天重复", Modifier.padding(top = 12.dp)) }
            if (weekly) OutlinedTextField(endDate, { endDate = it }, label = { Text("重复至 YYYY-MM-DD") }, singleLine = true)
            OutlinedTextField(location, { location = it }, label = { Text("教室 / 地点（或填写线上上课）") })
            OutlinedTextField(notes, { notes = it }, label = { Text("备注") })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (onDelete != null) TextButton(onClick = { deleting = true }) { Text("删除${if (initial.weekly) "整个重复系列" else "此记录"}") }
        }
    }, confirmButton = { TextButton(onClick = {
        val value = initial.copy(title = title.trim(), date = date.trim(), endDate = if (weekly) endDate.trim() else date.trim(),
            start = start.trim(), end = end.trim(), allDay = allDay, weekly = weekly, location = location.trim(), notes = notes.trim())
        error = runCatching { value.validate(); onSave(value) }.getOrElse { it.message ?: "保存失败，原记录已保留" }
        if (error == null) onClose()
    }) { Text("保存") } }, dismissButton = { TextButton(onClick = onClose) { Text("取消") } })
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("删除${initial.kind}？") },
        text = { Text(if (initial.weekly) "将删除此记录的所有重复日期。" else initial.title) },
        confirmButton = { TextButton(onClick = { error = onDelete?.invoke(); deleting = false; if (error == null) onClose() }) { Text("删除") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("取消") } })
}

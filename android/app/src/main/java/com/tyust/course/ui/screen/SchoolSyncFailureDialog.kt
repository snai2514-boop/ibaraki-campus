package com.tyust.course.ui.screen

import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import com.tyust.course.i18n.LocalizedText as Text
import com.tyust.course.academic.SchoolSyncFailures

@Composable
fun SchoolSyncFailureDialog() {
    val failures by SchoolSyncFailures.pending.collectAsState()
    val failure = failures.firstOrNull() ?: return
    AlertDialog(
        onDismissRequest = { SchoolSyncFailures.dismiss(failure) },
        title = { Text("同步失败：${failure.part}") },
        text = { Column {
            Text(failure.kind.label)
            Text(failure.reason)
            Text("已成功读取的数据已保存，可继续使用。失败项目可稍后重试。")
            if (failure.kind.feedback) com.tyust.course.i18n.SupportFooter()
        } },
        confirmButton = { TextButton(onClick = { SchoolSyncFailures.dismiss(failure) }) { Text("知道了") } }
    )
}

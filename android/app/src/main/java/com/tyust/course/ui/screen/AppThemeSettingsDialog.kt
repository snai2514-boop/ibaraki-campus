package com.tyust.course.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tyust.course.manager.AppThemeMode
import com.tyust.course.manager.AppearanceSettingsManager
import com.tyust.course.ui.system.SystemDialog
import com.tyust.course.ui.system.SystemPrimaryButton
import com.tyust.course.ui.system.SystemSecondaryButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import com.tyust.course.manager.ThemePack
import com.tyust.course.manager.ThemePackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppThemeSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    var preview by remember { mutableStateOf<ThemePack?>(null) }
    var error by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) coroutine.launch {
            importing = true
            runCatching { withContext(Dispatchers.IO) { ThemePackManager.read(context, uri) } }
                .onSuccess { preview = it; error = "" }.onFailure { error = it.message ?: "美化包读取失败" }
            importing = false
        }
    }
    SystemDialog(onDismissRequest = onDismiss, title = { Text("主题") }) {
        Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("更改主题会保留当前背景设置。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppThemeMode.entries.forEach { mode ->
                val select = { AppearanceSettingsManager.updateThemeMode(mode) }
                if (AppearanceSettingsManager.themeMode == mode) {
                    SystemPrimaryButton(text = "${mode.label} · 已选择", onClick = select, modifier = Modifier.fillMaxWidth())
                } else {
                    SystemSecondaryButton(text = mode.label, onClick = select, modifier = Modifier.fillMaxWidth())
                }
            }
            Text("美化包：${ThemePackManager.active?.name ?: "默认外观"}")
            SystemSecondaryButton(text = if (importing) "正在读取…" else "导入美化包 ZIP", onClick = {
                if (!importing) picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            }, modifier = Modifier.fillMaxWidth())
            preview?.let { pack ->
                val p = pack.light
                Column(Modifier.fillMaxWidth().background(Color(android.graphics.Color.parseColor(p.getValue("background")))).padding(12.dp)) {
                    Text(pack.name, color = Color(android.graphics.Color.parseColor(p.getValue("text"))))
                    Text("${pack.author} · 课程与学分预览", color = Color(android.graphics.Color.parseColor(p.getValue("primary"))))
                }
                SystemPrimaryButton(text = "应用这个美化包", onClick = {
                    runCatching { ThemePackManager.apply(context, pack) }.onSuccess { preview = null; error = "" }.onFailure { error = "美化包保存失败，原主题已保留" }
                }, modifier = Modifier.fillMaxWidth())
                SystemSecondaryButton(text = "取消预览", onClick = { preview = null }, modifier = Modifier.fillMaxWidth())
            }
            if (ThemePackManager.active != null) SystemSecondaryButton(text = "恢复默认外观", onClick = { ThemePackManager.clear(context); preview = null }, modifier = Modifier.fillMaxWidth())
            SystemSecondaryButton(text = "美化包制作教程", onClick = {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/snai2514-boop/ibaraki-campus/blob/main/THEME-PACKS.md")))
            }, modifier = Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            SystemSecondaryButton(text = "完成", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

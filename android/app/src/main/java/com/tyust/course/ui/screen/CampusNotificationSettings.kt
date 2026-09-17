package com.tyust.course.ui.screen

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.tyust.course.academic.SchoolNoticeNotifier
import com.tyust.course.i18n.LocalizedText as Text

@Composable
internal fun CampusNotificationSettings() {
    val context = LocalContext.current
    val notifier = remember { SchoolNoticeNotifier(context) }
    var enabled by remember { mutableStateOf(notifier.enabled) }
    var permission by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = granted; enabled = granted; notifier.enabled = granted
    }
    DisposableEffect(context) {
        val lifecycle = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if(event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permission = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("允许弹窗通知", Modifier.weight(1f))
                Switch(checked = enabled && permission, onCheckedChange = { value ->
                    if(!value) { enabled = false; notifier.enabled = false }
                    else {
                        notifier.channel()
                        if(Build.VERSION.SDK_INT >= 33 && !permission) request.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else if(permission) { enabled = true; notifier.enabled = true }
                        else context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName)))
                    }
                })
            }
            Text("每次打开自动同步；新公告、学分增加或 GPA 变化时提醒，首次读取不提醒历史变化。")
            Text("横幅弹窗由手机系统控制，可在系统通知设置中开启。", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                notifier.channel()
                if(Build.VERSION.SDK_INT >= 26) context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, SchoolNoticeNotifier.CHANNEL))
                else context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName)))
            }) { Text("系统通知设置") }
        }
    }
}

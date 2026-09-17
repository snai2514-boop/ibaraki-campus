package com.tyust.course

import com.tyust.course.i18n.LocalizedText as Text
import androidx.compose.material3.Text as RawText


import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import com.tyust.course.academic.SchoolStudentProfileStore
import com.tyust.course.academic.SchoolSyncState
import com.tyust.course.ui.system.GlassWindowHost
import com.tyust.course.ui.theme.CourseSelectorTheme

class IbarakiAccountActivity : ComponentActivity() {
    private var clearingSession by mutableStateOf(false)
    private var previewLogin by mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        com.tyust.course.ui.theme.StartupLogoAnimation.install(this)
        super.onCreate(savedInstanceState)
        previewLogin = intent.getBooleanExtra("previewLogin", false)
        if (intent.getBooleanExtra("logout", false)) {
            intent.removeExtra("logout")
            getSharedPreferences("school-access", MODE_PRIVATE).edit().putBoolean("signedOut", true).commit()
            SchoolSyncState.entered.value = false
            SchoolSyncState.running.value = false
            SchoolSyncState.message.value = ""
            SchoolSyncState.profile.value = null
            clearingSession = true
            android.webkit.CookieManager.getInstance().removeAllCookies {
                android.webkit.CookieManager.getInstance().flush()
                clearingSession = false
            }
        }
        setContent { CourseSelectorTheme { GlassWindowHost {
            com.tyust.course.ui.screen.SchoolSyncFailureDialog()
            val entered by SchoolSyncState.entered.collectAsState()
            val syncing by SchoolSyncState.running.collectAsState()
            val message by SchoolSyncState.message.collectAsState()
            androidx.activity.compose.BackHandler(enabled = previewLogin && entered) { previewLogin = false }
            if (entered && !previewLogin) {
                com.tyust.course.ui.screen.CampusHomeScreen()
            } else {
                Column(Modifier.fillMaxSize().background(if (com.tyust.course.manager.ThemePackManager.active != null || MaterialTheme.colorScheme.background.red < .4f) MaterialTheme.colorScheme.background else androidx.compose.ui.graphics.Color(0xFFF2F8FF)).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("茨城大学", modifier = Modifier.weight(1f), fontSize = 38.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold)
                        com.tyust.course.i18n.LanguageButton()
                    }
                    com.tyust.course.ui.screen.CampusIllustration(Modifier.fillMaxWidth().height(250.dp))
                    Text("教务助手", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
                    Text("登录后自动同步课表、日历、学分与 GPA。", modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center, fontSize = 18.sp, lineHeight = 28.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { previewLogin = false; startActivity(Intent(this@IbarakiAccountActivity, IbarakiPortalActivity::class.java)
                        .putExtra("backgroundSync", true)) }, enabled = !syncing && !clearingSession, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp)) {
                        Text(if (syncing) "正在同步…" else "学校账户登录", fontSize = 22.sp, lineHeight = 30.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                    if (syncing && message.isNotBlank()) Text(message, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(24.dp))
                    com.tyust.course.i18n.SupportFooter(Modifier.fillMaxWidth())
                }
            }
        } } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        previewLogin = intent.getBooleanExtra("previewLogin", false)
    }
    override fun onResume() {
        super.onResume()
        if (getSharedPreferences("school-access", MODE_PRIVATE).getBoolean("signedOut", false)) return
        runCatching { SchoolStudentProfileStore(this).load() }.onSuccess {
            SchoolSyncState.profile.value = it
            if (it != null) {
                // Saved local access is independent of the school's expiring web session.
                SchoolSyncState.entered.value = true
            }
        }
            .onFailure { SchoolSyncState.message.value = "学生资料读取失败，原记录已保留。" }
    }
}

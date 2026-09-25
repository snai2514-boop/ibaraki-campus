package com.tyust.course

import com.tyust.course.i18n.LocalizedText as Text
import androidx.compose.material3.Text as RawText


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tyust.course.academic.IbarakiPortalPolicy
import com.tyust.course.ui.system.GlassPageScaffold
import com.tyust.course.ui.theme.CourseSelectorTheme
import org.json.JSONTokener
import org.json.JSONObject
import org.json.JSONArray
import android.util.AtomicFile
import java.io.File
import com.tyust.course.academic.PortalImport
import com.tyust.course.academic.PortalImportParser
import com.tyust.course.academic.SchoolStudentProfileParser
import com.tyust.course.academic.SchoolStudentProfileStore
import com.tyust.course.academic.SchoolSyncState
import com.tyust.course.academic.SchoolSyncFailureKind

/** School-owned authentication stays in WebView; no password/cookie extraction or JS bridge. */
class IbarakiPortalActivity : ComponentActivity() {
    companion object { private var syncActive = false }
    private var ownsSync = false
    private var browser: WebView? = null
    private var status by mutableStateOf("请使用茨大 ID 和学校密码登录，并完成学校验证。")
    private var currentHost by mutableStateOf("csweb.ibaraki.ac.jp")
    private var readable by mutableStateOf(false)
    private var preview by mutableStateOf<String?>(null)
    private var reading by mutableStateOf(false)
    private var generation = 0
    private var candidate by mutableStateOf<PortalImport?>(null)
    private var ownerKey = ""
    private var saved by mutableStateOf<List<PortalImport>>(emptyList())
    private var autoSync by mutableStateOf(true)
    private var syncStep = -1
    private var noticeListSaved = false
    private var returnedHome = false
    private val backgroundSync get() = intent.getBooleanExtra("backgroundSync", false)
    private val silentSync get() = intent.getBooleanExtra("silentSync", false)
    private var authenticatedOnce = false
    private var authenticationVisible by mutableStateOf(false)
    private var openedAt = 0L
    private var stepStarted = 0L
    private var lastNavigation = 0L
    private val syncResults = mutableListOf<String>()
    private var parseIssue = ""
    private val deliveryLinks = JSONArray()
    private val syncHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val syncTick = object : Runnable {
        override fun run() {
            if (!autoSync || isDestroyed) return
            // Syllabus requests run only after all stateful school queries are finished.
            // Their timeout must never invalidate an already saved timetable.
            if(syncStep == 8) { readDeliveryBatch(); syncHandler.postDelayed(this,2500); return }
            // Run independently of page/JavaScript callbacks, which may never return.
            if (com.tyust.course.academic.SchoolSyncWatchdog.expired(android.os.SystemClock.elapsedRealtime(), stepStarted, authenticatedOnce)) {
                generation++; reading = false
                finishSyncStep("${com.tyust.course.academic.SchoolSyncFailures.part(syncStep, intent.hasExtra("syllabusCode"))} 未读取成功：学校响应超时，已保留原记录。")
                if (autoSync) syncHandler.postDelayed(this, 2500)
                return
            }
            if (autoSync && !authenticatedOnce && !authenticationVisible && android.os.SystemClock.elapsedRealtime() - openedAt > 35000) {
                stopSync("自动更新暂未完成：学校可能需要重新认证或网络不可用。已保留本机数据，可点击“从学校更新”重试。")
                return
            }
            if (autoSync && authenticatedOnce && !readable && stepStarted != 0L && android.os.SystemClock.elapsedRealtime() - stepStarted > 35000) {
                browser?.stopLoading()
                generation++; reading = false
                finishSyncStep("${com.tyust.course.academic.SchoolSyncFailures.part(syncStep)} 未读取成功：学校页面加载超时，已保留原记录。")
            }
            if (autoSync && syncStep < 9 && readable && !reading && preview == null) readPage(true)
            syncHandler.postDelayed(this, 2500)
        }
    }
    private val diagnosticSession = java.util.UUID.randomUUID().toString().take(8)
    private val diagnosticRecent = mutableMapOf<String, Pair<String, Long>>()
    private fun diagnostic(event: String, metadata: String = "") {
        val now = android.os.SystemClock.elapsedRealtime()
        if (event in setOf("page_finish", "page_read", "parse_miss")) {
            val signature = "$syncStep/$generation/$readable/$metadata"
            val previous = diagnosticRecent[event]
            if (previous?.first == signature && now - previous.second < 1000) return
            diagnosticRecent[event] = signature to now
        }
        com.tyust.course.utils.SyncDiagnostics.record(event,
            "session=$diagnosticSession step=$syncStep elapsed=${if (stepStarted == 0L) 0 else android.os.SystemClock.elapsedRealtime() - stepStarted} readable=$readable generation=$generation $metadata")
    }
    private val importFile get() = AtomicFile(File(noBackupFilesDir, "school-imports-v1.json"))

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (syncActive) { finish(); return }
        syncActive = true
        ownsSync = true
        autoSync = !intent.getBooleanExtra("manualRead", false)
        diagnostic(if (autoSync) "sync_start" else "manual_browser_start", "silent=$silentSync")
        com.tyust.course.academic.SchoolOpenSync.consume()
        openedAt = android.os.SystemClock.elapsedRealtime()
        if (autoSync) {
            if(!intent.getBooleanExtra("noticesOnly", false)) com.tyust.course.academic.DailySchoolSync(this).attempted()
            SchoolSyncState.running.value = true
            SchoolSyncState.details.value = ""
            SchoolSyncState.message.value = if (silentSync) "正在自动更新今日学校数据…" else "请完成学校登录。"
        }
        // Passwords, MFA and student records must not enter screenshots or recents thumbnails.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        runCatching { saved = com.tyust.course.academic.SchoolImportStore(this).load() }.onFailure { status = "本机记录读取失败，原文件已保留。" }
        val web = WebView(this).apply {
            alpha = if (intent.getBooleanExtra("manualRead", false)) 1f else 0f
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = block(request.url.toString())
                @Deprecated("Legacy WebView compatibility")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = block(url)
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    // Hide before the next document paints; returning from SSO must never
                    // flash the university dashboard while we detect its login state.
                    authenticationVisible = false
                    view.alpha = if (intent.getBooleanExtra("manualRead", false)) 1f else 0f
                    generation++; reading = false; readable = false
                    if (authenticatedOnce && stepStarted == 0L) stepStarted = android.os.SystemClock.elapsedRealtime()
                    currentHost = android.net.Uri.parse(url).host.orEmpty()
                    diagnostic("page_start", "schoolPage=${IbarakiPortalPolicy.allowsReading(url)} allowed=${IbarakiPortalPolicy.allowsNavigation(url)}")
                    status = "正在打开学校页面…"
                }
                override fun onPageFinished(view: WebView, url: String) {
                    readable = IbarakiPortalPolicy.allowsReading(url)
                    diagnostic("page_finish")
                    status = if (readable && autoSync) "正在同步学校数据…" else if (readable) "可手动读取当前页。" else "请在学校页面完成登录和验证。"
                    if (!silentSync && !authenticatedOnce && !readable && IbarakiPortalPolicy.allowsNavigation(url)) {
                        authenticationVisible = true
                        view.alpha = 1f
                    }
                    if (readable && autoSync && !reading) readPage(true)
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (!request.isForMainFrame || !autoSync || isFinishing) return
                    readable = false
                    diagnostic("network_error", "code=${error.errorCode}")
                    when (error.errorCode) {
                        ERROR_TIMEOUT -> stopSync("学校页面连接超时，请稍后重试。", SchoolSyncFailureKind.TIMEOUT)
                        ERROR_HOST_LOOKUP, ERROR_CONNECT, ERROR_IO -> stopSync("无法连接学校网站，请检查网络连接后重试。", SchoolSyncFailureKind.NETWORK)
                        else -> stopSync("学校页面加载失败（错误代码 ${error.errorCode}），请稍后重试。", SchoolSyncFailureKind.UNKNOWN)
                    }
                }
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                    handler.cancel(); readable = false; stopSync("学校页面证书验证失败，已停止连接。", SchoolSyncFailureKind.SECURITY)
                }
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    val crashed = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && detail.didCrash()
                    browser = null
                    (view.parent as? android.view.ViewGroup)?.removeView(view)
                    view.destroy()
                    setContent { }
                    stopSync(if (crashed) "学校网页组件发生异常，请重试或联系反馈。" else "系统释放了学校网页进程，请重新同步。",
                        if (crashed) SchoolSyncFailureKind.PROGRAM else SchoolSyncFailureKind.RESOURCE)
                    finish()
                    return true
                }
            }
        }
        browser = web
        setContent { CourseSelectorTheme {
            if (!intent.getBooleanExtra("manualRead", false)) {
                BackHandler { finish() }
                Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                    // Keep a full-sized, attached WebView so table layout and frame reads
                    // still work. Only its presentation is hidden, never its execution.
                    AndroidView(factory = { web }, modifier = Modifier.fillMaxSize())
                    if (!authenticationVisible || silentSync) Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text("茨城大学", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(24.dp))
                            CircularProgressIndicator()
                            Spacer(Modifier.height(24.dp))
                            Text("正在连接学校并更新资料…")
                            TextButton(onClick = { finish() }) { Text("取消") }
                        }
                    }
                }
            } else {
            BackHandler { if (preview != null) preview = null else if (web.canGoBack()) web.goBack() else finish() }
            GlassPageScaffold(title = "学校登录与读取", subtitle = currentHost, onBack = { finish() }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                    Text(status, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(checked = autoSync, onCheckedChange = {
                            autoSync = it
                            SchoolSyncState.running.value = it
                            if (it) { syncStep = -1; stepStarted = 0; ownerKey = ""; syncResults.clear(); web.loadUrl(IbarakiPortalPolicy.START_URL) }
                        })
                        Text("登录后自动同步课表、成绩与日历", style = MaterialTheme.typography.bodySmall)
                    }
                    if (syncStep == 9) Row {
                        TextButton(onClick = { startActivity(android.content.Intent(this@IbarakiPortalActivity, IbarakiTimetableActivity::class.java)) }) { Text("查看日历") }
                        TextButton(onClick = { startActivity(android.content.Intent(this@IbarakiPortalActivity, StudyProgressActivity::class.java)) }) { Text("查看学分与 GPA") }
                    }
                    if (preview == null && saved.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                            saved.forEach { item -> TextButton(onClick = { candidate = null; preview = item.title + "\n\n" + item.cards.joinToString("\n\n"); status = "本机已保存的学校数据" }) { Text(item.title) } }
                        }
                    }
                    if (preview == null) AndroidView(factory = { web }, modifier = Modifier.weight(1f).fillMaxWidth())
                    else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        Text("读取预览", style = MaterialTheme.typography.titleLarge)
                        Text("学校数据单独保存；手动记录保持原样。", Modifier.padding(vertical = 12.dp))
                        Text(preview.orEmpty())
                    }
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { if (preview != null) preview = null else web.loadUrl(IbarakiPortalPolicy.START_URL) }) {
                            Text(if (preview != null) "返回网页" else "重新打开")
                        }
                        Button(onClick = { autoSync = false; SchoolSyncState.running.value = false; readPage(false) }, enabled = readable && !reading && preview == null) { Text(if (reading) "读取中…" else "读取当前页") }
                    }
                    if (preview != null && candidate != null) Button(onClick = ::saveImport, modifier = Modifier.padding(horizontal = 12.dp)) { Text("确认保存到本机") }
                }
            }
            }
        } }
        web.loadUrl(IbarakiPortalPolicy.START_URL)
        syncHandler.postDelayed(syncTick, 2500)
        if (silentSync) {
            // Keep the authenticated WebView running behind the dashboard; never force an
            // expired-session login page onto a user browsing their local calendar.
            returnedHome = true
            val returnActivity = intent.getStringExtra("returnActivity")?.takeIf { it in setOf(
                IbarakiAccountActivity::class.java.name, StudyProgressActivity::class.java.name, IbarakiTimetableActivity::class.java.name)
            } ?: IbarakiAccountActivity::class.java.name
            startActivity(android.content.Intent().setClassName(this, returnActivity)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    private fun block(url: String): Boolean {
        val blocked = !IbarakiPortalPolicy.allowsNavigation(url)
        if (blocked) status = "此登录跳转暂未适配：${android.net.Uri.parse(url).host.orEmpty()}"
        return blocked
    }

    private fun readPage(automatic: Boolean) {
        val web = browser ?: return
        if (!IbarakiPortalPolicy.allowsReading(web.url.orEmpty()) || reading) return
        reading = true
        val stamp = generation
        // Only visible table text, never input values, cookies, URLs, hidden fields or authentication pages.
        web.evaluateJavascript("""
            (function() {
              if (location.protocol !== 'https:' || location.hostname !== 'csweb.ibaraki.ac.jp' || !location.pathname.startsWith('/campusweb/')) return {text:''};
              var output=[], timetableNames=[], count=0, authenticated=false,needsLogin=false,contactConfirmation=false;
              function visit(doc, depth) {
                if(depth>3 || count>=60000) return;
                var bodyText=(doc.body && doc.body.textContent || '').replace(/\s+/g,'');
                var noChange=Array.from(doc.querySelectorAll('button,input[type=submit],input[type=button],a')).some(function(n){
                  return (n.tagName==='INPUT'?n.value:n.textContent).replace(/\s+/g,'')==='変更なし';
                });
                if (noChange && bodyText.includes('本人連絡先') && bodyText.includes('変更する情報を入力し、変更ボタンをクリックしてください。')) contactConfirmation=true;
                if(Array.from(doc.querySelectorAll('a,button')).some(function(n){return n.textContent.replace(/\s+/g,'')==='ログアウト';})) authenticated=true;
                if(doc.querySelector('input[type=password]') || Array.from(doc.querySelectorAll('a,button')).some(function(n){return /^(ログイン|Login|Sign in)$/i.test(n.textContent.trim());})) needsLogin=true;
                doc.querySelectorAll('table.rishu-koma-inner').forEach(function(table) {
                  if(!table.getClientRects().length) return;
                  var lines=(table.innerText||'').split(/\r?\n/).map(function(v){return v.replace(/\s+/g,' ').trim();}).filter(Boolean);
                  if(lines.length>=3 && /^[A-Za-z0-9_-]{3,30}$/.test(lines[0]) && lines[1].length<=300 &&
                     lines.some(function(v){return /^[0-9.]+単位$/.test(v);}))
                    timetableNames.push({code:lines[0],name:lines[1]});
                });
                doc.querySelectorAll('table').forEach(function(table) {
                  if(count>=60000 || !table.getClientRects().length) return;
                  var rows=[], carry=[];
                  var gradeTable=Array.from(table.rows).some(function(row) {
                    var labels=Array.from(row.cells).map(function(c){return c.textContent.replace(/\s+/g,'');});
                    return labels.some(function(v){return /^(科目|科目名|授業科目名)$/.test(v);}) && labels.includes('合否') && labels.some(function(v){return /^(単位|単位数)$/.test(v);});
                  });
                  Array.from(table.rows).slice(0,300).forEach(function(row) {
                    var originals=Array.from(row.cells);
                    var cells=originals.map(function(cell) {
                      var clone=cell.cloneNode(true);
                      clone.querySelectorAll('input,textarea,select,script,style,table,[hidden]').forEach(function(n){n.remove();});
                      return clone.textContent.replace(/\s+/g,' ').trim().slice(0,500);
                    });
                    if(gradeTable) {
                      var expanded=[], column=0, next=[];
                      function inherited() {
                        while(carry[column] && carry[column].left>0) {
                          expanded[column]=carry[column].value;
                          if(carry[column].left>1) next[column]={value:carry[column].value,left:carry[column].left-1};
                          column++;
                        }
                      }
                      cells.forEach(function(value,i) {
                        inherited();
                        var cell=originals[i];
                        expanded[column]=value;
                        if(cell.colSpan>1) expanded[column]='[unsupported grade colspan]';
                        if(cell.rowSpan>1 && cell.rowSpan<=300) next[column]={value:value,left:cell.rowSpan-1};
                        column++;
                      });
                      inherited(); carry=next; cells=expanded;
                    }
                    var text=cells.join(' | '); if(text.trim()) rows.push(text);
                  });
                  var text=rows.join('\n'); if(text) { output.push(text); count+=text.length; }
                });
                doc.querySelectorAll('iframe,frame').forEach(function(frame) {
                  try { var child=frame.contentDocument; if(child && child.location.origin===location.origin) visit(child,depth+1); } catch(e) {}
                });
              }
              visit(document,0);
              return {text:output.join('\n\n').slice(0,60000),timetableNames:timetableNames,authenticated:authenticated,needsLogin:needsLogin,contactConfirmation:contactConfirmation};
            })();
        """.trimIndent()) { result ->
            if (stamp != generation || isDestroyed) return@evaluateJavascript
            reading = false
            if (!IbarakiPortalPolicy.allowsReading(web.url.orEmpty())) return@evaluateJavascript
            val text = runCatching { (JSONTokener(result).nextValue() as? JSONObject)?.optString("text").orEmpty() }.getOrDefault("")
            val courseNames=runCatching {
                val rows=(JSONTokener(result).nextValue() as? JSONObject)?.optJSONArray("timetableNames") ?: JSONArray()
                (0 until rows.length()).map {rows.getJSONObject(it)}.groupBy {it.getString("code")}
                    .mapNotNull { (code, values) -> values.map {it.getString("name")}.distinct().singleOrNull()?.let {code to it} }.toMap()
            }.getOrDefault(emptyMap())
            if (automatic) {
                val authenticated = runCatching { (JSONTokener(result).nextValue() as? JSONObject)?.optBoolean("authenticated") == true }.getOrDefault(false)
                val needsLogin = runCatching { (JSONTokener(result).nextValue() as? JSONObject)?.optBoolean("needsLogin") == true }.getOrDefault(false)
                val contactConfirmation = runCatching { (JSONTokener(result).nextValue() as? JSONObject)?.optBoolean("contactConfirmation") == true }.getOrDefault(false)
                if (autoSync && contactConfirmation) {
                    diagnostic("contact_confirmation_required")
                    stopSync("学校要求先确认联系方式，课表尚未读取。请打开学校网页核对资料；无需修改时选择「変更なし」，返回后重新更新。", SchoolSyncFailureKind.ACTION_REQUIRED)
                    return@evaluateJavascript
                }
                diagnostic("page_read", "chars=${text.length} authenticated=$authenticated needsLogin=$needsLogin")
                if (autoSync && authenticatedOnce && needsLogin && !authenticated) {
                    stopSync("学校登录已过期，请重新登录同步。", SchoolSyncFailureKind.AUTH)
                    return@evaluateJavascript
                }
                authenticationVisible = !silentSync && !authenticatedOnce && !authenticated && needsLogin
                web.alpha = if (authenticationVisible) 1f else 0f
                if (autoSync) try { processSync(text, authenticated, courseNames) }
                catch (e: Exception) { stopSync("读取程序发生异常（${e.javaClass.simpleName}），请联系反馈。", SchoolSyncFailureKind.PROGRAM) }
                return@evaluateJavascript
            }
            if (text.isBlank()) status = "当前页没有可读取的表格，请打开具体课表或成绩查询页。"
            else {
                candidate = null
                runCatching {
                    val student = Regex("学生番号 \\| ([A-Za-z0-9]+)").find(text)?.groupValues?.get(1) ?: error("无法确认学校账户，未保存")
                    ownerKey = java.security.MessageDigest.getInstance("SHA-256").digest(student.toByteArray()).joinToString("") { "%02x".format(it) }
                    val parsed = PortalImportParser.parse(text, courseNames=courseNames)
                    candidate = parsed.copy(key = "$ownerKey/${parsed.key}")
                    preview = parsed.title + "\n\n" + parsed.cards.joinToString("\n\n")
                    status = "已完成解析，请检查后保存。相同账户、相同学季再次保存会更新该快照。"
                }.onFailure { status = it.message ?: "无法解析此页面" }
            }
        }
        web.postDelayed({ if (stamp == generation && reading) { reading = false; status = "读取超时，请重试。" } }, 10000)
    }

    private fun saveImport() {
        val item = candidate ?: return
        runCatching {
            saved = com.tyust.course.academic.SchoolImportStore(this).save(item.copy(syncedAt = System.currentTimeMillis()))
            candidate = null; status = "已保存到本机，可在学业进度和课表页面查看学校数据。"
        }.onFailure { status = "保存失败，原有记录已保留，请重试。" }
    }

    private fun processSync(text: String, authenticated: Boolean, courseNames: Map<String,String>) {
        val web = browser ?: return
        val target = if (syncStep == -1) "profile" else if (syncStep == 0) "grades" else "Q$syncStep"
        val now = android.os.SystemClock.elapsedRealtime()
        if (authenticated) {
            authenticatedOnce = true
            getSharedPreferences("school-access", MODE_PRIVATE).edit().putBoolean("signedOut", false).apply()
        }
        if (authenticated && stepStarted == 0L) stepStarted = now
        if (authenticated && !returnedHome) {
            returnedHome = true
            SchoolSyncState.entered.value = true
            CookieManager.getInstance().flush()
            SchoolSyncState.message.value = "登录成功，正在后台同步学生资料与课程…"
            startActivity(android.content.Intent(this, IbarakiAccountActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .putExtra("previewLogin", false))
        }
        if (syncStep == 5) { readClassrooms(); return }
        if (syncStep == 6) { if(intent.hasExtra("syllabusCode")) readSyllabus() else readNotices(); return }
        if (syncStep == 7) { readRegistration(text); return }
        if (syncStep == 8) { readDeliveryBatch(); return }
        if (syncStep == -1) {
            val profile = SchoolStudentProfileParser.parse(text)
            if (profile != null) {
                val owner = java.security.MessageDigest.getInstance("SHA-256").digest(profile.studentNumber.toByteArray()).joinToString("") { "%02x".format(it) }
                runCatching { SchoolStudentProfileStore(this).save(profile); SchoolSyncState.profile.value = profile; ownerKey = owner }
                    .onSuccess { finishSyncStep("学生资料已更新") }
                    .onFailure { stopSync("学生资料保存失败，原记录已保留。") }
                return
            }
        }
        val parsed = runCatching { PortalImportParser.parse(text, allowEmptyTimetable = true, courseNames=courseNames) }
            .onFailure {
                if (text.isNotBlank()) diagnostic("parse_miss", "type=${it.javaClass.simpleName}")
                val targetPage = if (syncStep == 0) text.lines().any(com.tyust.course.academic.SchoolGradeTable::isHeader)
                    else syncStep in 1..4 && Regex("年度・学期 \\| \\d{4}年度 ${syncStep}クォーター").containsMatchIn(text)
                if (targetPage) parseIssue = it.message.orEmpty()
            }.getOrNull()
        val expected = parsed != null && if (syncStep == 0) parsed.key == "grades" else parsed.key.endsWith("-$target")
        if (expected) {
            val student = Regex("学生番号 \\| ([A-Za-z0-9]+)").find(text)?.groupValues?.get(1)
            if (student != null) {
                val owner = java.security.MessageDigest.getInstance("SHA-256").digest(student.toByteArray()).joinToString("") { "%02x".format(it) }
                if (ownerKey.isNotEmpty() && owner != ownerKey) { stopSync("学校账户发生变化，已停止同步，请重新打开。"); return }
                ownerKey = owner
                if (syncStep > 0 && parsed!!.lessons.isEmpty()) {
                    finishSyncStep("$target 学校暂无课程，保留旧记录")
                    return
                }
                val item = parsed!!.copy(key = "$owner/${parsed.key}", syncedAt = System.currentTimeMillis())
                val previousGrade = saved.firstOrNull { it.key == item.key }
                runCatching { saved = com.tyust.course.academic.SchoolImportStore(this).save(item) }
                    .onSuccess {
                        if(syncStep in 1..4) captureDeliveryLinks(item)
                        if (syncStep == 0) runCatching {
                            val changes = com.tyust.course.academic.AcademicChanges.changes(previousGrade, item)
                            com.tyust.course.academic.AcademicChangeInbox(this, ownerKey).append(changes)
                            if (changes.isNotEmpty()) com.tyust.course.academic.SchoolNoticeNotifier(this).send(changes.joinToString("\n"), 482, "学业资料变化")
                        }
                        finishSyncStep("${if (syncStep == 0) "成绩/学分/GPA" else target} 已更新 ${if (syncStep == 0) item.grades.size else item.lessons.size} 门") }
                    .onFailure { stopSync("保存失败，原记录已保留，请重试。") }
                return
            }
        }
        if (stepStarted != 0L && now - stepStarted > 35000) {
            finishSyncStep("$target 未读取成功：" + if (parseIssue.isNotBlank()) "数据校验失败，保留旧记录：$parseIssue" else "查询超时，保留旧记录")
            return
        }
        if (now - lastNavigation < 5000) return
        lastNavigation = now
        val stamp = generation
        val script = assets.open("school-sync-navigation.js").bufferedReader().use { it.readText() }
            .replace("__TARGET__", JSONObject.quote(target))
        web.evaluateJavascript(script) { clicked ->
            if (!autoSync || isDestroyed) return@evaluateJavascript
            diagnostic("navigation", "target=$target clicked=${clicked == "true"}")
            if (clicked == "true" && stepStarted == 0L) stepStarted = android.os.SystemClock.elapsedRealtime()
            if (stamp == generation) status = "正在同步 $target；请等待查询完成。"
        }
    }

    private fun finishSyncStep(message: String) {
        diagnostic("step_end", "failed=${message.contains("未读取成功")} kind=${com.tyust.course.academic.SchoolSyncFailures.classify(message)}")
        if (message.contains("未读取成功")) com.tyust.course.academic.SchoolSyncFailures.report(
            com.tyust.course.academic.SchoolSyncFailures.part(syncStep, intent.hasExtra("syllabusCode")), message)
        syncResults += message
        syncStep = if ((syncStep == 0 && intent.getBooleanExtra("gradesOnly", false)) ||
            (syncStep == 6 && (intent.getBooleanExtra("noticesOnly", false) || intent.hasExtra("syllabusCode")))) 9
            else if(syncStep == -1 && intent.getBooleanExtra("noticesOnly", false)) 6 else syncStep + 1
        // Publication can happen during holidays. Always query the school calendar.
        SchoolSyncState.details.value = syncResults.joinToString("；\n")
        parseIssue = ""
        stepStarted = android.os.SystemClock.elapsedRealtime(); lastNavigation = 0
        if (syncStep == 9) {
            autoSync = false
            status = syncResults.joinToString("；") + "。App 内日历随已适配课程自动更新。"
            SchoolSyncState.running.value = false
            val incomplete = syncResults.filter { it.contains("未读取成功") }
            if (incomplete.isEmpty() && !intent.getBooleanExtra("noticesOnly", false) && !intent.getBooleanExtra("gradesOnly", false)) com.tyust.course.academic.DailySchoolSync(this).succeeded()
            SchoolSyncState.message.value = if(intent.getBooleanExtra("noticesOnly", false) || intent.getBooleanExtra("gradesOnly", false)) syncResults.joinToString("；") else if (incomplete.isEmpty()) "同步完成 · 学生资料、课表、成绩与日历已更新。" +
                if (syncResults.any { it.startsWith("教室暂无") }) " 教室暂无新数据，已保留记录。" else ""
                else "部分同步完成：" + incomplete.joinToString("；")
            CookieManager.getInstance().flush()
            // Dispose of the background browser after saving, without interrupting the current screen.
            // Keep the school session so the next daily update can reuse it.
            if (SchoolSyncState.profile.value != null) SchoolSyncState.entered.value = true
            if (!returnedHome) {
                startActivity(android.content.Intent(this, IbarakiAccountActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    .putExtra("previewLogin", false))
            }
            finish()
        } else {
            status = syncResults.joinToString("；")
            // Return to the school home before switching between distinct query modules.
            SchoolSyncState.message.value = status + "；正在同步…"
            if (message.contains("未读取成功") || syncStep == 0 || syncStep == 1 || syncStep == 5 || syncStep == 6 || syncStep == 7 || syncStep == 8) browser?.loadUrl(IbarakiPortalPolicy.START_URL)
        }
    }

    private fun readRegistration(text: String) {
        val student = SchoolSyncState.profile.value?.studentNumber ?: return
        val now = android.os.SystemClock.elapsedRealtime()
        if(now - lastNavigation < 4000) return
        lastNavigation = now
        val stamp = generation
        val fallback=com.tyust.course.academic.SchoolRegistrationReading.readOnly(text,student)
        val request = JSONObject().put("action", "read").put("student", student).put("navigate",fallback==null)
        val script = assets.open("school-registration.js").bufferedReader().use {it.readText()}.replace("__REQUEST__", request.toString())
        browser?.evaluateJavascript(script) { value ->
            if(stamp != generation || syncStep != 7 || !autoSync || isDestroyed) return@evaluateJavascript
            var json = runCatching { JSONTokener(value).nextValue() as? JSONObject }.getOrNull() ?: return@evaluateJavascript
            if(!json.optBoolean("ready") && fallback!=null) json=com.tyust.course.academic.SchoolRegistrationReading.json(fallback)
            if(json.optBoolean("ready")) {
                runCatching { com.tyust.course.academic.SchoolRegistrationStore(this,student).save(json) }
                    .onSuccess {finishSyncStep(if(it.rows.any {r -> r.available}) "履修状况已更新：${it.rows.count {r -> r.available}} 门可登录课程" else "履修状况已读取；未确认可登记选项")}
                    .onFailure {finishSyncStep("履修状况未读取成功：原记录已保留")}
            }
        }
    }

    private fun captureDeliveryLinks(item: PortalImport) {
        val year=Regex("timetable-(\\d{4})-").find(item.key)?.groupValues?.get(1)?.toIntOrNull() ?: return
        val request=JSONObject().put("capture",true).put("year",year).put("codes",JSONArray(item.lessons.map {it.description.substringBefore(' ')}.distinct()))
        val script=assets.open("school-course-modes.js").bufferedReader().use {it.readText()}.replace("__REQUEST__",request.toString())
        browser?.evaluateJavascript(script) {value ->
            if(isDestroyed) return@evaluateJavascript
            val rows=runCatching {(JSONTokener(value).nextValue() as? JSONObject)?.optJSONArray("links")}.getOrNull() ?: return@evaluateJavascript
            for(i in 0 until rows.length()) deliveryLinks.put(rows.getJSONArray(i))
        }
    }

    private fun readDeliveryBatch() {
        if(syncStep!=8 || !autoSync) return
        val now=android.os.SystemClock.elapsedRealtime()
        if(now-stepStarted>30000 || deliveryLinks.length()==0) {finishSyncStep("授课方式暂无可核实的新数据，保留旧记录"); return}
        if(!readable || now-lastNavigation<2500) return
        lastNavigation=now
        val request=JSONObject().put("links",deliveryLinks)
        val script=assets.open("school-course-modes.js").bufferedReader().use {it.readText()}.replace("__REQUEST__",request.toString())
        browser?.evaluateJavascript(script) {value ->
            if(syncStep!=8 || !autoSync || isDestroyed) return@evaluateJavascript
            val json=runCatching {JSONTokener(value).nextValue() as? JSONObject}.getOrNull() ?: return@evaluateJavascript
            if(json.optBoolean("ready") || android.os.SystemClock.elapsedRealtime()-stepStarted>25000) {
                val rows=json.optJSONArray("rows") ?: JSONArray()
                runCatching {
                    val store=com.tyust.course.academic.SchoolDeliveryStore(this,ownerKey)
                    (0 until rows.length()).map {rows.getJSONObject(it)}.groupBy {it.getInt("year")}.forEach { (year, values) -> store.save(year,JSONArray(values)) }
                }.onSuccess {finishSyncStep("授课方式已核对 ${rows.length()} 门")}
                    .onFailure {finishSyncStep("授课方式保存失败，已保留原课表和授课方式")}
            }
        }
    }

    private fun readClassrooms() {
        val web = browser ?: return
        if(stepStarted == 0L) stepStarted = android.os.SystemClock.elapsedRealtime()
        if(android.os.SystemClock.elapsedRealtime()-stepStarted>35000) { finishSyncStep("教室未读取成功：读取超时，已保留旧记录"); return }
        val stamp = generation
        val script = assets.open("school-classrooms.js").bufferedReader().use { it.readText() }
        web.evaluateJavascript(script) { value ->
            if(stamp!=generation || !autoSync || syncStep!=5 || isDestroyed) return@evaluateJavascript
            val json = runCatching { JSONTokener(value).nextValue() as? JSONObject }.getOrNull() ?: return@evaluateJavascript
            if(!json.optBoolean("ready")) return@evaluateJavascript
            val rows = json.optJSONArray("rows") ?: JSONArray()
            val published = json.optJSONArray("events") ?: JSONArray()
            val meetings = (0 until published.length()).mapNotNull { i -> runCatching {
                val row = published.getJSONObject(i)
                com.tyust.course.academic.SchoolLiveMeeting(row.getString("name"), row.getString("date"), row.getInt("period"))
            }.getOrNull() }
            val readings = (0 until rows.length()).mapNotNull { i -> runCatching { val r=rows.getJSONObject(i)
                com.tyust.course.academic.SchoolClassroomReading(r.optString("code"),r.getString("date"),r.getInt("period"),r.getString("room"),r.optString("name")) }.getOrNull() }
            val liveSources = com.tyust.course.academic.SchoolLiveCalendar.sources(ownerKey, saved, meetings)
            val liveRooms = readings.filter { it.name.isNotBlank() }.mapNotNull { reading ->
                liveSources.singleOrNull { source -> source.event.date == reading.date && source.event.lesson.period == reading.period &&
                    com.tyust.course.academic.SchoolLiveCalendar.matchesLesson(source.event.lesson, reading.name)
                }?.let { source -> com.tyust.course.academic.SchoolClassroomMatch.key(source.key, source.event.lesson.description.substringBefore(' '), reading.date, reading.period) to reading.room }
            }.groupBy({ it.first }, { it.second }).mapNotNull { (key, rooms) -> rooms.distinct().singleOrNull()?.let { key to it } }.toMap()
            val updates = com.tyust.course.academic.SchoolClassroomMatch.updates(ownerKey, saved, readings, SchoolSyncState.profile.value) + liveRooms
            diagnostic("classrooms_read", "rows=${readings.size} matched=${updates.size}")
            if (liveSources.isNotEmpty()) runCatching { com.tyust.course.academic.SchoolLiveCalendarStore(this, ownerKey).merge(meetings) }
                .onFailure { diagnostic("live_calendar_save_failed", "type=${it.javaClass.simpleName}") }
            if(updates.isEmpty()) { finishSyncStep("教室暂无可核实的新数据，保留旧记录"); return@evaluateJavascript }
            runCatching { com.tyust.course.academic.SchoolClassroomStore(this).merge(updates) }
                .onSuccess { finishSyncStep("教室已更新 ${updates.keys.map { it.split('/').takeLast(3) }.distinct().size} 条") }
                .onFailure { stopSync("教室保存失败，原记录已保留。") }
        }
    }

    private fun readSyllabus() {
        val web = browser ?: return
        val code = intent.getStringExtra("syllabusCode").orEmpty()
        val year = intent.getIntExtra("syllabusYear", 0)
        val quarter = intent.getIntExtra("syllabusQuarter", 0)
        if(ownerKey.isBlank() || !code.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")) || year !in 2000..2100 || quarter !in 1..4) { finishSyncStep("课程大纲未读取成功"); return }
        val now = android.os.SystemClock.elapsedRealtime()
        if(stepStarted == 0L) stepStarted = now
        if(now-stepStarted>45000) { finishSyncStep("课程大纲未读取成功，保留旧记录"); return }
        if(now-lastNavigation<2500) return
        lastNavigation=now
        val request=JSONObject().put("code",code).put("year",year).put("quarter",quarter)
        val script=assets.open("school-syllabus.js").bufferedReader().use { it.readText() }.replace("__REQUEST__",request.toString())
        val stamp=generation
        web.evaluateJavascript(script) { result ->
            if(stamp!=generation || !autoSync || syncStep!=6 || isDestroyed) return@evaluateJavascript
            val data=runCatching { JSONTokener(result).nextValue() as? JSONObject }.getOrNull() ?: return@evaluateJavascript
            if(data.optBoolean("error")) { finishSyncStep("课程大纲未读取成功，请确认学校已有该年度课程"); return@evaluateJavascript }
            if(data.optBoolean("ready")) {
                runCatching { com.tyust.course.academic.SchoolSyllabusStore(this,ownerKey).save(year,code,data) }
                    .onSuccess { finishSyncStep("课程大纲已更新") }
                    .onFailure { finishSyncStep("课程大纲未读取成功，保留旧记录") }
            } else if(data.optBoolean("navigate")) {
                val nav=assets.open("school-sync-navigation.js").bufferedReader().use { it.readText() }.replace("__TARGET__",JSONObject.quote("Q$quarter"))
                web.evaluateJavascript(nav,null)
            }
        }
    }

    private fun readNotices() {
        val web = browser ?: return
        if(ownerKey.isBlank()) { finishSyncStep("公告未读取成功，保留旧记录"); return }
        if(stepStarted == 0L) stepStarted = android.os.SystemClock.elapsedRealtime()
        if(android.os.SystemClock.elapsedRealtime() - stepStarted > 45000) { finishSyncStep("公告未读取成功，保留旧记录"); return }
        val now = android.os.SystemClock.elapsedRealtime()
        if(now - lastNavigation < 2500) return
        lastNavigation = now
        val id = intent.getStringExtra("noticeId").orEmpty().takeIf { it.matches(Regex("[0-9]*")) }.orEmpty()
        val script = assets.open("school-notices.js").bufferedReader().use { it.readText() }
            .replace("__DETAIL__", JSONObject.quote(if(noticeListSaved) id else ""))
        val stamp = generation
        web.evaluateJavascript(script) { result ->
            if(stamp != generation || !autoSync || syncStep != 6 || isDestroyed) return@evaluateJavascript
            val json = runCatching { JSONTokener(result).nextValue() as? JSONObject }.getOrNull() ?: return@evaluateJavascript
            android.util.Log.d("SchoolNoticeSync", "ready=${json.optBoolean("ready")} phase=${json.optString("phase")} articles=${json.optJSONArray("articles")}")
            if(!json.optBoolean("ready")) return@evaluateJavascript
            val store = com.tyust.course.academic.SchoolNoticeStore(this, ownerKey)
            runCatching {
                if(json.has("body")) {
                    val body = json.getString("body")
                    require(store.load().rows.any { it.id == id && body.contains(it.title) })
                    store.saveBody(id, body)
                    finishSyncStep("公告正文已更新")
                } else {
                    val array = json.optJSONArray("rows") ?: error("Missing notice list")
                    val rows = (0 until array.length()).map { array.getJSONObject(it).let { row ->
                        com.tyust.course.academic.SchoolNotice(row.getString("id"),row.getString("title"),row.getString("published"),row.getString("period"))
                    } }
                    val previous = store.load()
                    store.saveList(rows, json.optBoolean("complete"))
                    com.tyust.course.academic.SchoolNoticeNotifier(this).publish(previous, rows)
                    noticeListSaved = true
                    if(id.isEmpty()) finishSyncStep("公告已更新 ${rows.size} 条")
                    else if(rows.none { it.id == id }) finishSyncStep("公告正文未读取成功，可能已下架")
                }
            }.onFailure { finishSyncStep("公告未读取成功，保留旧记录") }
        }
    }

    private fun stopSync(message: String, kind: SchoolSyncFailureKind = com.tyust.course.academic.SchoolSyncFailures.classify(message)) { diagnostic("sync_stop", "kind=$kind"); com.tyust.course.academic.SchoolSyncFailures.report(com.tyust.course.academic.SchoolSyncFailures.part(syncStep, intent.hasExtra("syllabusCode")), message, kind); autoSync = false; generation++; reading = false; syncHandler.removeCallbacksAndMessages(null); status = message; SchoolSyncState.running.value = false; SchoolSyncState.message.value = message; SchoolSyncState.details.value = syncResults.joinToString("；\n"); if(backgroundSync || returnedHome || !intent.getBooleanExtra("manualRead", false)) finish() }
    override fun onDestroy() { if (!ownsSync) { super.onDestroy(); return }; diagnostic("destroy", "unfinished=$autoSync"); syncActive = false; if(autoSync) { SchoolSyncState.running.value = false; SchoolSyncState.message.value = "同步已停止，可点击学校登录重试。" }; syncHandler.removeCallbacksAndMessages(null); generation++; browser?.apply { stopLoading(); destroy() }; browser = null; super.onDestroy() }
}

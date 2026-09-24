package com.tyust.course

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tyust.course.academic.*
import com.tyust.course.i18n.LocalizedText as Text
import com.tyust.course.ui.theme.CourseSelectorTheme
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** A separate explicit registration session; foreground synchronization never submits. */
class IbarakiRegistrationActivity : ComponentActivity() {
    private lateinit var web: WebView
    private lateinit var store: SchoolRegistrationStore
    private var student = ""
    private var snapshot by mutableStateOf<RegistrationSnapshot?>(null)
    private var selected by mutableStateOf<Set<String>>(emptySet())
    private var status by mutableStateOf("正在读取履修状况…")
    private var busy by mutableStateOf(false)
    private var manualWeb by mutableStateOf(false)
    private var confirm by mutableStateOf(false)
    private var sent by mutableStateOf(false)
    private var readyToSubmit by mutableStateOf(false)
    private var generation = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var nativeStage = "start"
    private var nativeScope = ""
    private var nativeSlots = listOf<String>()
    private var nativeIndex = 0
    private var nativeFailures = 0
    private val nativeRows = linkedMapOf<String, JSONObject>()
    private var nativeTarget: RegistrationCourse? = null
    private var batch: RegistrationBatch? = null
    private var batchReport by mutableStateOf("")
    private var dispatchGeneration = -1
    private var finalReason = ""
    private var nativeDeadline = 0L
    private var nativeEvaluating = false
    private var nativePageLoading = true
    private var nativeEvalToken = 0
    private var nativeCancelled = false
    private val nativeTick = Runnable { readNative() }

    private fun nextNative(stage: String) {
        nativeStage=stage; nativeDeadline=android.os.SystemClock.elapsedRealtime()+30_000
        com.tyust.course.utils.SyncDiagnostics.record("registration_stage","stage=$stage completed=$nativeIndex total=${nativeSlots.size} rows=${nativeRows.size}")
        handler.removeCallbacks(nativeTick); handler.postDelayed(nativeTick,250)
    }

    private fun refreshNative() {
        if(sent) return
        batch=null
        handler.removeCallbacksAndMessages(null); nativeEvaluating=false; nativeEvalToken++
        nativeScope=""; nativeRows.clear(); nativeIndex=0; nativeFailures=0; nativeSlots=emptyList()
        selected=emptySet(); readyToSubmit=false; busy=true; nativeCancelled=false; status="正在读取学校可登录课程…"
        nativePageLoading=true;nextNative("start"); web.loadUrl(IbarakiPortalPolicy.START_URL)
    }

    private fun stopNative(message: String) {
        val activeBatch=batch
        val summary=if(activeBatch!=null) {
            batch=activeBatch.stop()
            activeBatch.report(message)
        } else message
        if(activeBatch!=null) {
            batchReport=summary
            runCatching {store.saveBatchReport(summary)}
        }
        com.tyust.course.utils.SyncDiagnostics.record("registration_stop","stage=$nativeStage completed=$nativeIndex total=${nativeSlots.size} pending=$sent")
        handler.removeCallbacks(nativeTick); nativeStage="done"; nativeEvaluating=false; nativeEvalToken++
        busy=false; readyToSubmit=false; status=summary
    }

    /** Read-only recovery/final verification. No submission is resumed from storage. */
    private fun queryBatchResult(reason: String = "") {
        batch=(batch ?: store.recoverPendingBatch())?.stop()
        if(batch==null || store.pendingScope().isBlank()) {
            stopNative("登记记录不完整，无法自动查询；请打开学校原页查看结果。")
            return
        }
        finalReason=reason
        nativeScope=store.pendingScope(); nativeTarget=null; nativeCancelled=false
        handler.removeCallbacksAndMessages(null);nativeEvaluating=false;nativeEvalToken++
        busy=true;readyToSubmit=false;status="正在统一查询所有所选课程的登记结果…"
        nativePageLoading=true;nextNative("verifyStart");web.loadUrl(IbarakiPortalPolicy.START_URL)
    }

    private fun finishBatchVerification(response: JSONObject) {
        val active=batch ?: return stopNative("登记记录无法读取，请重新查询。")
        val codes=response.optJSONArray("registered") ?: return stopNative("学校未返回完整登记结果，请重新查询。")
        val updated=active.verify((0 until codes.length()).map {codes.getString(it)}.toSet())
        val message=if(finalReason.isNotBlank()) "$finalReason\n已统一查询学校课表。"
            else "已统一查询学校登记结果。返回首页可同步正式课表。"
        val report=updated.report(message)
        runCatching {store.completeSubmission(report)}.onFailure {
            stopNative("已查询学校结果，但本机保存失败，可重新查询。");return
        }
        batch=updated;batchReport=report;sent=false;selected=emptySet()
        snapshot=snapshot?.copy(rows=snapshot!!.rows.map {if(it.id in updated.confirmedIds) it.copy(available=false,status="已登记") else it})
        nativeTarget=null;stopNative(message)
    }

    private fun finishNativeScan() {
        val count=nativeRows.values.count {it.optBoolean("available")}
        val message="已读取 $count 门可登录课程" + if(nativeFailures>0) "；$nativeFailures 个时间格读取失败，可刷新重试。" else "；点击时间格选择，登记资格以学校最终结果为准。"
        val json=JSONObject().put("ready",true).put("student",student).put("scope",nativeScope)
            .put("signature","native:$nativeScope").put("message",message).put("rows",JSONArray(nativeRows.values.toList()))
        runCatching {store.save(json)}.onSuccess {
            snapshot=it; selected=emptySet(); nativeStage="done"; busy=false; readyToSubmit=true; status=message
            com.tyust.course.utils.SyncDiagnostics.record("registration_complete","slots=${nativeSlots.size} failed=$nativeFailures candidates=$count")
        }.onFailure {stopNative("课程保存失败，已保留旧记录。")}
    }

    private fun nativeReadFailed(message: String) {
        if(sent && batch!=null && nativeStage!="verifyStart") {
            queryBatchResult("登记流程已停止：$message")
            return
        }
        if(Regex("账户|登录|学期已变化|会话").containsMatchIn(message)) {stopNative(message);return}
        if(nativeStage in setOf("open","back") && nativeTarget==null) {
            if(nativeStage=="open") {nativeFailures++;nativeIndex++}
            status="部分时间格读取失败，继续读取其余课程…"
            nativePageLoading=true;nextNative("recover"); web.loadUrl(IbarakiPortalPolicy.START_URL)
        } else stopNative(message)
    }

    private fun readNative() {
        if(isDestroyed || manualWeb || nativeStage in setOf("done","waiting") || nativeEvaluating) return
        if(nativeDeadline==0L) nativeDeadline=android.os.SystemClock.elapsedRealtime()+30_000
        if(android.os.SystemClock.elapsedRealtime()>nativeDeadline) {nativeReadFailed("学校课程读取超时，请刷新重试。");return}
        if(nativePageLoading || !IbarakiPortalPolicy.allowsReading(web.url.orEmpty())) {
            handler.postDelayed(nativeTick,1000);return
        }
        val stage=nativeStage
        val action=when(stage) {"start","recover","submitStart","verifyStart" -> "start";"open","submitOpen" -> "open";"back","resultBack" -> "back";"submitSend" -> "submit";else -> "read"}
        val request=JSONObject().put("action",action).put("student",student)
        if(nativeScope.isNotBlank()) request.put("scope",nativeScope)
        if(stage=="open") request.put("slot",nativeSlots.getOrNull(nativeIndex).orEmpty())
        if(stage in setOf("submitOpen","submitSend")) nativeTarget?.let {
            request.put("slot",it.slotKey).put("id",it.id).put("signature",it.signature)
        }
        val stamp=generation
        val token=++nativeEvalToken
        nativeEvaluating=true; busy=true
        if(stage=="submitSend") {
            dispatchGeneration=generation
            nativeStage="waiting" // Never retry a potentially dispatched registration.
        }
        val script=assets.open("school-registration-native.js").bufferedReader().use {it.readText()}.replace("__REQUEST__",request.toString())
        web.evaluateJavascript(script) {value ->
            if(token!=nativeEvalToken) return@evaluateJavascript
            nativeEvaluating=false
            if(isDestroyed || manualWeb || nativeCancelled) return@evaluateJavascript
            if(stamp!=generation && stage!="submitSend") {handler.postDelayed(nativeTick,500);return@evaluateJavascript}
            val response=runCatching {JSONTokener(value).nextValue() as? JSONObject}.getOrNull()
            if(response==null) {nativeReadFailed("未能解析学校课程，请刷新重试。");return@evaluateJavascript}
            when(response.optString("kind")) {
                "error" -> {
                    nativeReadFailed(response.optString("message","课程读取失败，请刷新"))
                }
                "sent" -> {
                    val updated=runCatching {batch!!.dispatched(nativeTarget!!.id)}.getOrElse {
                        nativeReadFailed("登记队列状态异常。");return@evaluateJavascript
                    }
                    batch=updated
                    runCatching {store.savePendingBatch(updated,nativeScope)}.onFailure {
                        nativeReadFailed("登记进度保存失败。");return@evaluateJavascript
                    }
                    status="已发送 ${updated.sentIds.size} / ${updated.courses.size} 门，全部发送后统一查询结果…"
                    nextNative("result")
                }
                "grid" -> {
                    if(stage=="verifyStart") {
                        finishBatchVerification(response)
                    } else if(stage in setOf("result","resultBack")) {
                        // Wait for the school response, but do not verify/promote each course here.
                        if(generation<=dispatchGeneration) {handler.postDelayed(nativeTick,500);return@evaluateJavascript}
                        nativeTarget=batch?.current
                        if(nativeTarget==null) queryBatchResult()
                        else nextNative("submitOpen")
                    } else if(stage=="submitStart") {
                        nextNative("submitOpen")
                    } else if(stage=="start") {
                        nativeScope=response.getString("scope")
                        val slots=response.getJSONArray("slots")
                        nativeSlots=(0 until slots.length()).map {slots.getJSONObject(it).getString("key")}
                        if(nativeSlots.isEmpty()) finishNativeScan() else nextNative("open")
                    } else if(stage in setOf("back","recover")) {
                        if(nativeIndex>=nativeSlots.size) finishNativeScan() else nextNative("open")
                    }
                }
                "list" -> {
                    if(stage in setOf("open","submitOpen")) {
                        if(stage=="submitOpen") {
                            val target=nativeTarget ?: return@evaluateJavascript
                            val rows=response.getJSONArray("rows")
                            val matched=(0 until rows.length()).map {rows.getJSONObject(it)}.singleOrNull {it.optString("id")==target.id}
                            if(matched==null || !matched.optBoolean("available") || matched.optString("signature")!=target.signature) {
                                nativeReadFailed("课程信息或开放状态已变化，请刷新后重新选择。");return@evaluateJavascript
                            }
                            val updated=runCatching {batch!!.beginCurrent(target.id)}.getOrElse {
                                nativeReadFailed("登记队列状态异常。");return@evaluateJavascript
                            }
                            runCatching {store.savePendingBatch(updated,nativeScope)}.onFailure {nativeReadFailed("登记状态保存失败，当前课程未提交。");return@evaluateJavascript}
                            batch=updated;sent=true;nativeCancelled=false;nextNative("submitSend")
                        } else {
                            val rows=response.getJSONArray("rows")
                            for(i in 0 until rows.length()) {
                                val row=rows.getJSONObject(i);val id=row.getString("id")
                                if(id !in nativeRows) nativeRows[id]=row
                                else {
                                    val old=nativeRows.getValue(id)
                                    val schedule=old.optString("schedule").split(" / ").toMutableSet()
                                    schedule.add(row.optString("schedule"));old.put("schedule",schedule.filter {it.isNotBlank()}.joinToString(" / "))
                                }
                            }
                            snapshot=RegistrationSnapshot(student,nativeScope,"","native:$nativeScope",nativeRows.values.map {r ->
                                RegistrationCourse(r.getString("id"),r.getString("code"),r.getString("name"),r.optString("schedule"),r.optString("credits"),r.optString("status"),r.optBoolean("available"),
                                    r.optString("teacher"),r.optString("remote"),r.optString("slotKey"),r.optString("signature"),r.optString("faculty"))
                            },0)
                            nativeIndex++
                            status="正在读取可登录课程：$nativeIndex / ${nativeSlots.size}，已找到 ${nativeRows.size} 门"
                            nextNative("back")
                        }
                    } else if(stage=="result") {
                        if(generation>dispatchGeneration) nextNative("resultBack")
                        else handler.postDelayed(nativeTick,500)
                    }
                }
                else -> handler.postDelayed(nativeTick,700)
            }
        }
        handler.postDelayed({
            if(token==nativeEvalToken && (nativeEvaluating || nativeStage=="waiting")) {
                nativeEvaluating=false;nativeReadFailed(if(sent) "学校未及时返回登记响应。" else "学校课程读取超时，请刷新重试。")
            }
        },30_000)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        student = SchoolStudentProfileStore(this).load()?.studentNumber.orEmpty()
        if(student.isBlank() || SchoolSyncState.running.value) { finish(); return }
        SchoolOpenSync.consume()
        store=SchoolRegistrationStore(this,student)
        batchReport=store.batchReport()
        snapshot=store.load()
        sent=store.pending()
        if(sent) {status="正在查询上次登记结果…"}
        web=WebView(this).apply {
            alpha=0f
            importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            settings.javaScriptEnabled=true; settings.domStorageEnabled=true
            settings.allowFileAccess=false; settings.allowContentAccess=false
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.useWideViewPort=true; settings.loadWithOverviewMode=true
            settings.setSupportZoom(true); settings.builtInZoomControls=true; settings.displayZoomControls=false
            CookieManager.getInstance().setAcceptCookie(true)
            webViewClient=object: WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = !IbarakiPortalPolicy.allowsNavigation(request.url.toString())
                @Deprecated("Legacy WebView")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !IbarakiPortalPolicy.allowsNavigation(url)
                override fun onPageStarted(view: WebView, url: String, icon: android.graphics.Bitmap?) {
                    generation++; readyToSubmit=false; nativeEvaluating=false; nativePageLoading=true
                }
                override fun onPageFinished(view: WebView, url: String) {
                    nativePageLoading=false
                    if(manualWeb) { prepareSchoolPage(); return }
                    if(nativeStage!="done") {handler.removeCallbacks(nativeTick);handler.postDelayed(nativeTick,300)}
                }
                override fun onReceivedSslError(view: WebView, h: SslErrorHandler, e: android.net.http.SslError) {
                    h.cancel(); stopNative("学校证书异常，已停止连接。")
                }
            }
            webChromeClient=object: WebChromeClient() {
                override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                    if((!sent && !manualWeb) || !IbarakiPortalPolicy.allowsReading(url)) {result.cancel(); return true}
                    if(!manualWeb) {
                        result.cancel()
                        handler.post {cancelNativeSubmission()}
                        return true
                    }
                    android.app.AlertDialog.Builder(this@IbarakiRegistrationActivity).setTitle("学校登记确认")
                        .setMessage(message).setPositiveButton("确认") { _, _ -> result.confirm() }
                        .setNegativeButton("取消") { _, _ -> result.cancel(); cancelNativeSubmission() }.setOnCancelListener {result.cancel();cancelNativeSubmission()}.show()
                    return true
                }
            }
        }
        setContent { CourseSelectorTheme {
            BackHandler(enabled=manualWeb) { leaveSchoolPage() }
            Box(Modifier.fillMaxSize()) {
                // The school session remains alive, but registration is presented by native UI.
                AndroidView(factory={web},modifier=Modifier.fillMaxSize().safeDrawingPadding().padding(top=if(manualWeb) 96.dp else 0.dp),update={
                    it.alpha=if(manualWeb) 1f else 0f
                    it.importantForAccessibility=if(manualWeb) android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO else android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                })
                if(manualWeb) Surface(Modifier.fillMaxWidth().safeDrawingPadding()) {
                    Column(Modifier.padding(horizontal=12.dp)) {
                        Row {
                            TextButton(onClick={leaveSchoolPage()}) {Text("返回课程预览")}
                            TextButton(onClick={if(web.canGoBack()) web.goBack()}) {Text("网页返回")}
                        }
                        Text("点击时间格查看课程；登记后请核对学校结果。",style=MaterialTheme.typography.bodySmall)
                    }
                }
                if(!manualWeb) Surface(Modifier.fillMaxSize()) {
                    com.tyust.course.ui.screen.RegistrationWeekScreen(snapshot,selected,status,
                        readyToSubmit,busy,sent,onToggle={course,checked ->
                            selected=if(checked) selected+course.id else selected-course.id
                        },onRead={nativeTarget=null;nativeCancelled=false;refreshNative()},onSubmit={confirm=true},onBack={finish()},
                        onResultChecked={queryBatchResult()},
                        onLogin={startActivity(android.content.Intent(this@IbarakiRegistrationActivity,IbarakiPortalActivity::class.java).putExtra("backgroundSync",true))},
                        onSchoolWeb={
                            if(batch!=null && nativeStage!="done") stopNative("已切换至学校原页。")
                            handler.removeCallbacksAndMessages(null);busy=false;readyToSubmit=false;manualWeb=true;prepareSchoolPage()
                        },lastResult=batchReport)
                }
            }
            if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("确认登录这些课程？")},text={
                Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState())) {
                    Text("共选择 ${selected.size} 门课程。确认一次后依次提交，全部处理后自动统一查询结果。连接中断或课程信息变化时停止发送，并统一查询已发送课程。请确认时间冲突、先修条件与个人学分上限。")
                    snapshot?.rows?.filter {it.id in selected}?.forEach {
                        Text("${it.code} ${it.name}\n${it.schedule}" + if(it.credits.isNotBlank()) " · ${it.credits} 学分" else "")
                        if(it.faculty.isNotBlank()) Text("开课学部：${it.faculty}")
                    }
                }
            },confirmButton={TextButton(onClick={confirm=false;submit()}) {Text("确认并提交")}},dismissButton={TextButton(onClick={confirm=false}) {Text("取消")}})
        } }
        if(sent) queryBatchResult("已恢复上次记录，未重复提交任何课程。")
        else refreshNative()
    }

    private fun prepareSchoolPage() {
        if(!manualWeb || !IbarakiPortalPolicy.allowsReading(web.url.orEmpty())) return
        // The school's mobile wrapper can collapse below its floating table. Keep the
        // original form and handlers intact, while making every column scrollable.
        web.evaluateJavascript("""
            (() => {
              if (!document.body || !document.body.innerText.includes('履修登録')) return;
              if (document.getElementById('campus-registration-layout')) return;
              const style = document.createElement('style');
              style.id = 'campus-registration-layout';
              style.textContent = '.sp-table-scroll-box{height:auto!important;max-height:none!important}' +
                'div.sp-table-scroll{display:block!important;height:auto!important;max-height:none!important;overflow-x:auto!important;overflow-y:hidden!important}' +
                'div.sp-table-scroll>table{display:table!important;position:static!important;float:none!important}';
              document.head.appendChild(style);
            })()
        """.trimIndent(), null)
    }

    private fun leaveSchoolPage() {
        manualWeb=false; selected=emptySet()
        nativeTarget=null; if(!sent) refreshNative()
    }

    private fun cancelNativeSubmission() {
        if(manualWeb) return
        nativeCancelled=true;nativeTarget=null
        queryBatchResult("学校提示需要额外确认，已停止后续发送。")
    }

    private fun submit() {
        val s=snapshot ?: return
        if(!readyToSubmit || sent || busy || selected.isEmpty() || !s.signature.startsWith("native:")) return
        if(!RegistrationPolicy.fresh(s.time,System.currentTimeMillis())) {readyToSubmit=false; status="预览已过期，请刷新后确认。"; return}
        val approved=runCatching {RegistrationBatch.approve(s,selected)}.getOrElse {status="所选课程已变化，请刷新后重新选择。";return}
        val report=approved.report("已确认选择，登记结果尚待学校确认。")
        runCatching {store.saveBatchReport(report)}.onFailure {status="登记清单保存失败，未提交。";return}
        batch=approved;batchReport=report;nativeTarget=approved.current
        nativeScope=s.scope;readyToSubmit=false;busy=true;nativeCancelled=false
        nativePageLoading=true;status="正在重新核对所选课程…";nextNative("submitStart");web.loadUrl(IbarakiPortalPolicy.START_URL)
    }
    override fun onDestroy() {handler.removeCallbacksAndMessages(null); if(::web.isInitialized) {web.stopLoading(); web.destroy()}; super.onDestroy()}
}

package com.tyust.course

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
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
    private var showWeb by mutableStateOf(false)
    private var confirm by mutableStateOf(false)
    private var sent by mutableStateOf(false)
    private var readyToSubmit by mutableStateOf(false)
    private var generation = 0
    private var navigations = 0
    private var readDeadline = 0L
    private var resultDeadline = 0L
    private var finalConfirmationSent = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        student = SchoolStudentProfileStore(this).load()?.studentNumber.orEmpty()
        if(student.isBlank() || SchoolSyncState.running.value) { finish(); return }
        SchoolOpenSync.consume()
        store=SchoolRegistrationStore(this,student)
        snapshot=store.load()
        sent=store.pending()
        if(sent) {showWeb=true; status="上次登记请求尚待核对，请先查询学校登记结果。"}
        web=WebView(this).apply {
            alpha=0f
            importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            settings.javaScriptEnabled=true; settings.domStorageEnabled=true
            settings.allowFileAccess=false; settings.allowContentAccess=false
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.useWideViewPort=true; settings.loadWithOverviewMode=true
            CookieManager.getInstance().setAcceptCookie(true)
            webViewClient=object: WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = !IbarakiPortalPolicy.allowsNavigation(request.url.toString())
                @Deprecated("Legacy WebView")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !IbarakiPortalPolicy.allowsNavigation(url)
                override fun onPageStarted(view: WebView, url: String, icon: android.graphics.Bitmap?) {
                    generation++; readyToSubmit=false; busy=false
                }
                override fun onPageFinished(view: WebView, url: String) {
                    if(!sent) read() else { status="正在核对学校登记结果…"; handler.postDelayed({pollResult()},1000) }
                }
                override fun onReceivedSslError(view: WebView, h: SslErrorHandler, e: android.net.http.SslError) {
                    h.cancel(); busy=false; status="学校证书异常，已停止连接。"
                }
            }
            webChromeClient=object: WebChromeClient() {
                override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                    if(!sent || !IbarakiPortalPolicy.allowsReading(url)) {result.cancel(); return true}
                    android.app.AlertDialog.Builder(this@IbarakiRegistrationActivity).setTitle("学校登记确认")
                        .setMessage(message).setPositiveButton("确认") { _, _ -> result.confirm() }
                        .setNegativeButton("取消") { _, _ -> result.cancel() }.setOnCancelListener {result.cancel()}.show()
                    return true
                }
            }
        }
        setContent { CourseSelectorTheme {
            Box(Modifier.fillMaxSize()) {
                // The school session remains alive, but registration is presented by native UI.
                AndroidView(factory={web},modifier=Modifier.fillMaxSize())
                Surface(Modifier.fillMaxSize()) {
                    com.tyust.course.ui.screen.RegistrationWeekScreen(snapshot,selected,status,
                        readyToSubmit,busy,sent,onToggle={course,checked ->
                            selected=if(checked) selected+course.id else selected-course.id
                        },onRead={readDeadline=0L;navigations=0;read()},onSubmit={confirm=true},onBack={finish()},
                        onResultChecked={store.markPending(false);sent=false;readDeadline=0L;read()},
                        onLogin={startActivity(android.content.Intent(this@IbarakiRegistrationActivity,IbarakiPortalActivity::class.java).putExtra("backgroundSync",true))})
                }
            }
            if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("确认登录这些课程？")},text={
                Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState())) {
                    Text("只提交下列已勾选课程。请核对时间冲突、先修条件与个人学分上限。")
                    snapshot?.rows?.filter {it.id in selected}?.forEach {Text("${it.code} ${it.name}\n${it.schedule} · ${it.credits} 学分")}
                }
            },confirmButton={TextButton(onClick={confirm=false;submit()}) {Text("确认并提交")}},dismissButton={TextButton(onClick={confirm=false}) {Text("取消")}})
        } }
        web.loadUrl(IbarakiPortalPolicy.START_URL)
    }

    private fun evaluate(request: JSONObject, callback: (JSONObject) -> Unit) {
        if(!IbarakiPortalPolicy.allowsReading(web.url.orEmpty())) { busy=false; showWeb=true; status="请在学校页面完成登录，然后重新读取。"; return }
        val stamp=generation
        busy=true
        val script=assets.open("school-registration.js").bufferedReader().use {it.readText()}.replace("__REQUEST__",request.put("student",student).toString())
        web.evaluateJavascript(script) {value ->
            if(isDestroyed) return@evaluateJavascript
            busy=false
            if(stamp!=generation && request.optString("action") !in setOf("submit","confirm")) return@evaluateJavascript
            val json=runCatching {JSONTokener(value).nextValue() as? JSONObject}.getOrNull()
            if(json==null) {status="未能确认学校响应，请查看学校页面。"; showWeb=true} else callback(json)
        }
        handler.postDelayed({if(busy && stamp==generation) {busy=false; readyToSubmit=false; status="学校响应超时，请核对学校页面。"; showWeb=true}},15000)
    }
    private fun read() {
        if(busy || sent) return
        if(readDeadline==0L) readDeadline=android.os.SystemClock.elapsedRealtime()+30000
        readyToSubmit=false
        evaluate(JSONObject().put("action","read").put("navigate",navigations < 2)) {response ->
            val fallback=SchoolRegistrationReading.readOnly(response.optString("tableText"),student)
            val json=if(!response.optBoolean("ready") && fallback!=null) SchoolRegistrationReading.json(fallback) else response
            if(json.optBoolean("ready")) runCatching { store.save(json) }.onSuccess {
                selected=RegistrationPolicy.reconcile(selected,snapshot,it); snapshot=it; readyToSubmit=true; status=it.message; showWeb=false
            }.onFailure {status="履修状况保存失败，已保留旧记录。"}
            else {status=json.optString("message","正在打开履修状况…"); showWeb=true
                if(json.optBoolean("navigated")) navigations++
                if(android.os.SystemClock.elapsedRealtime()<readDeadline) handler.postDelayed({if(!isDestroyed) read()},1500)
            }
        }
    }
    private fun submit() {
        val s=snapshot ?: return
        if(!readyToSubmit || sent || busy || selected.isEmpty()) return
        if(!RegistrationPolicy.fresh(s.time,System.currentTimeMillis())) {readyToSubmit=false; status="预览已过期，请重新读取后确认。"; return}
        runCatching {store.beginSubmission(selected,s.scope)}.onFailure {status="登记状态保存失败，未提交。"; return}
        resultDeadline=0L; finalConfirmationSent=false
        sent=true; readyToSubmit=false
        evaluate(JSONObject().put("action","submit").put("signature",s.signature).put("ids",JSONArray(selected.toList()))) {json ->
            status=json.optString("message","结果未知，请核对学校页面，勿重复提交。")
            showWeb=true
            // A failed preflight is safe to refresh; uncertain/sent requests stay locked.
            if(!json.optBoolean("sent") && json.has("ready")) {store.markPending(false); sent=false}
            else {status="已提交勾选课程，正在核对学校结果…"; handler.postDelayed({pollResult()},1000)}
        }
    }
    private fun pollResult() {
        if(!sent || isDestroyed) return
        if(resultDeadline==0L) resultDeadline=android.os.SystemClock.elapsedRealtime()+30000
        if(android.os.SystemClock.elapsedRealtime()>resultDeadline) {status="学校登记结果尚未确认，已停止自动操作；请核对后再试，避免重复登记。"; return}
        if(busy) {handler.postDelayed({pollResult()},1500); return}
        val request=JSONObject().put("action","result").put("navigate",false).put("ids",JSONArray(store.pendingIds().toList())).put("scope",store.pendingScope())
        evaluate(request) {response ->
            when(response.optString("result")) {
                "success" -> {
                    store.markPending(false); sent=false; selected=emptySet()
                    runCatching {snapshot=store.save(response)}
                    readyToSubmit=false; status="学校已确认：勾选课程登记成功。请更新正式课表。"
                }
                "confirm" -> if(!finalConfirmationSent) {
                    finalConfirmationSent=true
                    evaluate(request.put("action","confirm").put("signature",response.getString("signature"))) {
                        status="正在核对学校最终登记结果…"; handler.postDelayed({pollResult()},1500)
                    }
                } else handler.postDelayed({pollResult()},1500)
                else -> handler.postDelayed({pollResult()},1500)
            }
        }
    }
    override fun onDestroy() {handler.removeCallbacksAndMessages(null); if(::web.isInitialized) {web.stopLoading(); web.destroy()}; super.onDestroy()}
}

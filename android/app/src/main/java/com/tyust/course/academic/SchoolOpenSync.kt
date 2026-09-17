package com.tyust.course.academic

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/** Foreground visits, not activity navigation, trigger a full school sync. */
object SchoolOpenSync {
    private var started = 0
    private var background = true
    private val visits = SchoolSyncVisit()
    fun consume(): Boolean = visits.consume()
    fun install(app: Application) {
        val handler = Handler(Looper.getMainLooper())
        val markBackground = Runnable { if (started == 0) background = true }
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(a: Activity) {
                handler.removeCallbacks(markBackground)
                if (background) { visits.opened(); background = false }
                started++
            }
            override fun onActivityStopped(a: Activity) {
                started = (started - 1).coerceAtLeast(0)
                if (started == 0 && !a.isChangingConfigurations) handler.postDelayed(markBackground, 700)
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityResumed(a: Activity) {
                if (a.javaClass.name !in setOf("com.tyust.course.IbarakiAccountActivity", "com.tyust.course.StudyProgressActivity", "com.tyust.course.IbarakiTimetableActivity")) return
                if (a.intent.getBooleanExtra("previewLogin", false) || a.getSharedPreferences("school-access", 0).getBoolean("signedOut", false)) return
                val profile = SchoolSyncState.profile.value ?: runCatching { SchoolStudentProfileStore(a).load() }.getOrNull() ?: return
                SchoolSyncState.profile.value = profile
                if (!consume() || SchoolSyncState.running.value) return
                SchoolSyncState.running.value = true
                a.startActivity(android.content.Intent(a, com.tyust.course.IbarakiPortalActivity::class.java)
                    .putExtra("backgroundSync", true).putExtra("silentSync", true)
                    .putExtra("returnActivity", a.javaClass.name))
            }
            override fun onActivityPaused(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
}

package com.tyust.course

import android.app.Application
import com.tyust.course.manager.AppearanceSettingsManager
import com.tyust.course.manager.AppThemeCoordinator
import com.tyust.course.ui.system.GlassRuntimeGuard

class CourseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.tyust.course.utils.SyncDiagnostics.initialize(this)
        com.tyust.course.academic.SchoolOpenSync.install(this)
        com.tyust.course.i18n.AppLanguage.initialize(this)
        GlassRuntimeGuard.initialize(this)
        AppearanceSettingsManager.initialize(this)
        com.tyust.course.manager.ThemePackManager.initialize(this)
        AppThemeCoordinator.initialize(this)
        val processName = if (android.os.Build.VERSION.SDK_INT >= 28) getProcessName() else {
            getSystemService(android.app.ActivityManager::class.java).runningAppProcesses
                ?.firstOrNull { it.pid == android.os.Process.myPid() }?.processName
        }
        if (processName == packageName) {
            com.tyust.course.schedule.ScheduleReminderScheduler.get(this).start(this)
            com.tyust.course.usage.UsageStatsManager.initialize(this)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        AppThemeCoordinator.configurationChanged()
    }
}

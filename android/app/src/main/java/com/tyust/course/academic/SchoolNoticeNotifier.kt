package com.tyust.course.academic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tyust.course.IbarakiAccountActivity
import com.tyust.course.R

class SchoolNoticeNotifier(private val context: Context) {
    companion object {
        const val CHANNEL = "school-notices"
        fun changed(previous: SchoolNotices, current: List<SchoolNotice>): List<SchoolNotice> {
            if(previous.syncedAt == 0L) return emptyList()
            val old = previous.rows.associateBy { it.id }
            return current.filter { old[it.id]?.let { p -> p.published != it.published || p.title != it.title } ?: true }
        }
    }
    private val prefs = context.getSharedPreferences("school-notifications", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply(); if(!value) { NotificationManagerCompat.from(context).cancel(481); NotificationManagerCompat.from(context).cancel(482); NotificationManagerCompat.from(context).cancel(483) } }
    fun channel() {
        if(android.os.Build.VERSION.SDK_INT < 26) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "履修・成績", NotificationManager.IMPORTANCE_HIGH))
    }
    fun publish(previous: SchoolNotices, current: List<SchoolNotice>) {
        // The first successful import establishes a baseline, rather than alerting for old posts.
        val fresh = changed(previous, current)
        if(fresh.isNotEmpty()) send(fresh.first().title)
    }
    fun send(title: String, id: Int = 481, heading: String = "履修・成績") {
        if(!enabled || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        channel()
        val body = if (id == 482) com.tyust.course.i18n.AppLanguage.text(title) else title
        val intent = Intent(context, IbarakiAccountActivity::class.java).putExtra(if(id == 483) "openRegistration" else "openNotices", true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_campus_mark)
            .setContentTitle(com.tyust.course.i18n.AppLanguage.text(heading)).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        try { NotificationManagerCompat.from(context).notify(id, notification) } catch (_: SecurityException) { }
    }
}

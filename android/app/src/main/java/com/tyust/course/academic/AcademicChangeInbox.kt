package com.tyust.course.academic

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AcademicChangeInbox(context: Context, private val owner: String) {
    private val prefs = context.getSharedPreferences("academic-change-inbox", Context.MODE_PRIVATE)
    fun rows(): List<String> = runCatching {
        val a = JSONArray(prefs.getString(owner, "[]"))
        (0 until a.length()).map { a.getString(it) }
    }.getOrDefault(emptyList())
    fun append(messages: List<String>) {
        if (messages.isEmpty()) return
        val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ROOT).format(java.util.Date())
        prefs.edit().putString(owner, JSONArray((messages.map { "$time\n$it" } + rows()).take(40)).toString()).apply()
    }
}

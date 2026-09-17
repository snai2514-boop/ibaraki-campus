package com.tyust.course.academic

import android.content.Context
import android.util.AtomicFile
import java.io.File
import org.json.JSONObject

/** User-confirmed classroom supplements, scoped to the account, year, quarter and course. */
class SchoolClassroomStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "school-classrooms-v1.json"))
    fun load(): Map<String, String> {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return emptyMap()
        val json = JSONObject(file.openRead().bufferedReader().use { it.readText() })
        return json.keys().asSequence().associateWith { json.getString(it) }
    }
    fun put(key: String, room: String): Map<String, String> {
        val next = load().toMutableMap().apply { if(room.isBlank()) remove(key) else put(key, room.trim()) }
        return write(next)
    }
    fun merge(updates: Map<String, String>): Map<String, String> = write(load() + updates.filterValues { it.isNotBlank() })
    private fun write(next: Map<String, String>): Map<String, String> {
        val stream = file.startWrite()
        try { stream.write(JSONObject(next as Map<*, *>).toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
        return next
    }
}

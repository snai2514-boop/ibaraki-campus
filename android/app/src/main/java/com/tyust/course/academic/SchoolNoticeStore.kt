package com.tyust.course.academic

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SchoolNotice(val id: String, val title: String, val published: String, val period: String, val body: String = "")
data class SchoolNotices(val rows: List<SchoolNotice> = emptyList(), val syncedAt: Long = 0, val complete: Boolean = false)

/** Per-school-account local cache. Never persist the portal's temporary session URLs. */
class SchoolNoticeStore(context: Context, owner: String) {
    init { require(Regex("[a-f0-9]{64}").matches(owner)) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "notices-$owner.json"))
    fun load(): SchoolNotices {
        if (!file.baseFile.exists()) return SchoolNotices()
        val json = JSONObject(file.openRead().bufferedReader().use { it.readText() })
        val rows = json.getJSONArray("rows")
        return SchoolNotices((0 until rows.length()).map { i -> rows.getJSONObject(i).let {
            SchoolNotice(it.getString("id"), it.getString("title"), it.getString("published"), it.getString("period"), it.optString("body"))
        } }, json.optLong("syncedAt"), json.optBoolean("complete"))
    }
    fun saveList(rows: List<SchoolNotice>, complete: Boolean) {
        val old = load().rows.associateBy { it.id }
        require(rows.size <= 1000 && rows.all { Regex("[0-9]+").matches(it.id) && it.title.isNotBlank() })
        val incoming = rows.map { row -> row.copy(body = old[row.id]?.takeIf { it.published == row.published && it.title == row.title }?.body.orEmpty()) }
        val result = if(complete) incoming else (incoming + old.values.filter { oldRow -> incoming.none { it.id == oldRow.id } })
        write(SchoolNotices(result.sortedByDescending { it.published }, System.currentTimeMillis(), complete))
    }
    fun saveBody(id: String, body: String) {
        require(body.isNotBlank() && body.length <= 100000)
        val old = load(); require(old.rows.any { it.id == id })
        write(old.copy(rows = old.rows.map { if(it.id == id) it.copy(body = body) else it }))
    }
    private fun write(value: SchoolNotices) {
        val rows = JSONArray()
        value.rows.forEach { rows.put(JSONObject().put("id",it.id).put("title",it.title).put("published",it.published).put("period",it.period).put("body",it.body)) }
        val bytes = JSONObject().put("rows", rows).put("syncedAt",value.syncedAt).put("complete",value.complete).toString().toByteArray()
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) } catch(e: Exception) { file.failWrite(stream); throw e }
    }
}

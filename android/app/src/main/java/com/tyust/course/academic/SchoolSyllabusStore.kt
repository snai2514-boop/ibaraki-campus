package com.tyust.course.academic

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

class SchoolSyllabusStore(private val context: Context, private val owner: String) {
    private fun file(year: Int, code: String): AtomicFile {
        require(owner.matches(Regex("[a-f0-9]{64}")) && year in 2000..2100 && code.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")))
        return AtomicFile(File(context.noBackupFilesDir, "syllabus-$owner-$year-$code.json"))
    }
    fun load(year: Int, code: String): JSONObject? = runCatching { JSONObject(file(year,code).openRead().bufferedReader().use { it.readText() }) }.getOrNull()
    fun save(year: Int, code: String, data: JSONObject) {
        require(data.getString("code") == code && data.getString("title").isNotBlank())
        require(data.toString().length < 500000)
        data.put("syncedAt",System.currentTimeMillis())
        val target=file(year,code); val stream=target.startWrite()
        try { stream.write(data.toString().toByteArray(Charsets.UTF_8)); target.finishWrite(stream) }
        catch(e: Exception) { target.failWrite(stream); throw e }
        SchoolDeliveryStore(context,owner).save(year,org.json.JSONArray().put(data))
    }
}

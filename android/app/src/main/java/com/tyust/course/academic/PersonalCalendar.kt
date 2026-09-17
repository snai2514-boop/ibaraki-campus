package com.tyust.course.academic

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID

/** Personal entries have their own store; a school refresh cannot replace them. */
data class PersonalCalendarEntry(
    val id: String = UUID.randomUUID().toString(), val title: String, val kind: String,
    val date: String, val endDate: String = date, val weekly: Boolean = false,
    val start: String = "08:40", val end: String = "10:25", val allDay: Boolean = false,
    val location: String = "", val notes: String = ""
) {
    fun validate() {
        require(title.isNotBlank() && title.length <= 100) { "请填写名称（最多 100 字）" }
        require(kind in listOf("课程", "事件"))
        require(IbarakiTimetable.parseDate(date) != null && IbarakiTimetable.parseDate(endDate) != null && endDate >= date) { "请填写有效日期，结束日期不能早于开始日期" }
        require(!weekly || (IbarakiTimetable.parseDate(endDate)!!.timeInMillis - IbarakiTimetable.parseDate(date)!!.timeInMillis) / 86400000 <= 366) { "重复范围最多一年" }
        require(allDay || validTime(start) && validTime(end) && start < end) { "请填写有效时间 HH:mm，结束时间应晚于开始时间" }
    }
    fun occursOn(value: String): Boolean {
        if (!weekly) return value == date
        val day = IbarakiTimetable.parseDate(value) ?: return false
        return value in date..endDate && day.get(Calendar.DAY_OF_WEEK) == IbarakiTimetable.parseDate(date)!!.get(Calendar.DAY_OF_WEEK)
    }
    companion object { fun validTime(value: String) = Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(value) }
}

class PersonalCalendarStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "personal-calendar-v1.json"))
    fun load(): List<PersonalCalendarEntry> {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return emptyList()
        val rows = JSONArray(file.openRead().bufferedReader().use { it.readText() })
        return (0 until rows.length()).map { i -> rows.getJSONObject(i).let { r ->
            PersonalCalendarEntry(r.getString("id"), r.getString("title"), r.getString("kind"), r.getString("date"), r.getString("endDate"),
                r.getBoolean("weekly"), r.getString("start"), r.getString("end"), r.getBoolean("allDay"), r.getString("location"), r.getString("notes")).also { it.validate() }
        } }
    }
    fun put(entry: PersonalCalendarEntry): List<PersonalCalendarEntry> { entry.validate(); return write(load().filterNot { it.id == entry.id } + entry) }
    fun delete(id: String) = write(load().filterNot { it.id == id })
    private fun write(entries: List<PersonalCalendarEntry>): List<PersonalCalendarEntry> {
        val rows = JSONArray()
        entries.forEach { e -> rows.put(JSONObject().put("id", e.id).put("title", e.title).put("kind", e.kind).put("date", e.date)
            .put("endDate", e.endDate).put("weekly", e.weekly).put("start", e.start).put("end", e.end).put("allDay", e.allDay).put("location", e.location).put("notes", e.notes)) }
        val stream = file.startWrite()
        try { stream.write(rows.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
        return entries
    }
}

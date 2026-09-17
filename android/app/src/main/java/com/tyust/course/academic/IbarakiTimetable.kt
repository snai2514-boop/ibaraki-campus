package com.tyust.course.academic

import com.tyust.course.schedule.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

data class IbarakiTimetable(val firstMonday: String = "", val courses: List<ScheduleCourseRecord> = emptyList()) {
    fun validate() {
        require(firstMonday.isEmpty() || Regex("\\d{4}-\\d{2}-\\d{2}").matches(firstMonday) && ScheduleDates.firstMonday(firstMonday, zone) != null) {
            "请填写有效的第 1 日历周周一日期（YYYY-MM-DD），作为本机周次起点"
        }
        require(courses.map { it.id }.distinct().size == courses.size) { "课表记录标识重复" }
        courses.forEach { course ->
            require(course.id.isNotBlank()) { "课表记录标识缺失" }
            val error = validateScheduleCourse(course, 5)
            require(error == null) { error ?: "课表无效" }
        }
    }
    fun onDate(date: String): List<ScheduleCourseRecord> {
        val calendar = parseDate(date) ?: return emptyList()
        val week = ScheduleDates.weekAt(firstMonday, calendar.timeInMillis, zone) ?: return emptyList()
        val day = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        return courses.filter { it.day == day && week in ScheduleWeeks.parse(it.weeks).weeks }
            .sortedBy { it.startPeriod }
    }
    companion object {
        val zone: TimeZone get() = TimeZone.getTimeZone("Asia/Tokyo")
        // Official 2026 engineering handbook, printed page 3.
        val periodTimes = mapOf(1 to ("08:40" to "10:25"), 2 to ("10:35" to "12:20"),
            3 to ("13:10" to "14:55"), 4 to ("15:05" to "16:50"), 5 to ("17:00" to "18:45"))
        fun parseDate(text: String): Calendar? = runCatching {
            require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(text))
            val fields = text.split('-').map(String::toInt)
            Calendar.getInstance(zone).apply {
                clear(); isLenient = false; set(fields[0], fields[1] - 1, fields[2]); timeInMillis
            }
        }.getOrNull()
    }
}

object IbarakiTimetableCodec {
    fun encode(timetable: IbarakiTimetable): String {
        timetable.validate()
        val courses = JSONArray()
        timetable.courses.forEach { course -> courses.put(JSONObject().put("id", course.id).put("name", course.name)
            .put("teacher", course.teacher).put("location", course.location).put("day", course.day)
            .put("start", course.startPeriod).put("end", course.endPeriod).put("weeks", course.weeks)) }
        return JSONObject().put("version", 1).put("firstMonday", timetable.firstMonday).put("courses", courses).toString()
    }
    fun decode(text: String): IbarakiTimetable {
        val root = JSONObject(text)
        require(root.getInt("version") == 1) { "不支持的课表版本" }
        val rows = root.getJSONArray("courses")
        return IbarakiTimetable(root.getString("firstMonday"), (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            ScheduleCourseRecord(row.getString("id"), row.getString("name"), row.getString("teacher"), row.getString("location"),
                row.getInt("day"), row.getInt("start"), row.getInt("end"), row.getString("weeks"), true)
        }).also { it.validate() }
    }
}

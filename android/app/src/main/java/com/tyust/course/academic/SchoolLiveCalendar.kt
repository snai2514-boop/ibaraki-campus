package com.tyust.course.academic

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Dated meetings published by the school, independent of our reviewed 2026 calendar. */
data class SchoolLiveMeeting(val name: String, val date: String, val period: Int)

object SchoolLiveCalendar {
    fun normalize(name: String) = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKC)
        .replace('〜', '~').replace(Regex("\\s+"), "")

    /** New snapshots keep the exact visible course-name line separately from the teacher. */
    fun matchesLesson(lesson: PortalLesson, calendarName: String): Boolean =
        if(lesson.courseName.isNotBlank()) normalize(lesson.courseName)==normalize(calendarName)
        else matchesName(lesson.description,calendarName)

    /** Timetable cells append instructor names; calendar entries omit that suffix. */
    fun matchesName(description: String, calendarName: String): Boolean {
        val body = java.text.Normalizer.normalize(description.substringAfter(' ').substringBeforeLast(' '), java.text.Normalizer.Form.NFKC).replace('〜', '~')
        val wanted = normalize(calendarName)
        if (wanted.isBlank()) return false
        var matched = 0
        for ((index, char) in body.withIndex()) {
            if (char.isWhitespace()) continue
            if (matched >= wanted.length || char != wanted[matched++]) return false
            if (matched == wanted.length) {
                val tail = body.substring(index + 1)
                return tail.isBlank() || (Regex("【(?:[1-4]Q|前期|後期|前学期|後学期|通年)】").containsMatchIn(wanted) && tail.first().isWhitespace())
            }
        }
        return false
    }

    fun sources(owner: String, snapshots: List<PortalImport>, meetings: List<SchoolLiveMeeting>): List<SchoolCalendarNavigation.Source> {
        if (owner.isBlank()) return emptyList()
        return meetings.mapNotNull { meeting ->
            val date = IbarakiTimetable.parseDate(meeting.date) ?: return@mapNotNull null
            if (meeting.period !in 1..5 || meeting.name.isBlank()) return@mapNotNull null
            val year = date.get(java.util.Calendar.YEAR) - if (date.get(java.util.Calendar.MONTH) < 3) 1 else 0
            val matches = snapshots.filter { it.key.matches(Regex("${Regex.escape(owner)}/timetable-$year-Q[1-4]")) }
                .flatMap { it.lessons }.filter { lesson ->
                    lesson.period == meeting.period &&
                        matchesLesson(lesson, meeting.name)
                }.distinctBy { it.description }
            // Same course in adjacent quarters is fine; different codes with the same name are ambiguous.
            val lesson = matches.singleOrNull() ?: return@mapNotNull null
            SchoolCalendarNavigation.Source("$owner/timetable-$year-calendar", SchoolCalendarEvent(meeting.date,
                lesson.copy(day = (date.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 + 1), false))
        }.distinctBy { listOf(it.key, it.event.date, it.event.lesson.period, it.event.lesson.description) }
    }
}

class SchoolLiveCalendarStore(context: Context, private val owner: String) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "school-live-calendar-$owner.json"))
    fun load(): List<SchoolLiveMeeting> {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return emptyList()
        val rows = JSONArray(file.openRead().bufferedReader().use { it.readText() })
        return (0 until rows.length()).map { i -> rows.getJSONObject(i).let {
            SchoolLiveMeeting(it.getString("name"), it.getString("date"), it.getInt("period"))
        } }
    }
    fun merge(meetings: List<SchoolLiveMeeting>) {
        val publishedDates = meetings.map { it.date }.toSet()
        val next = (meetings + load().filterNot { it.date in publishedDates }).distinctBy { listOf(it.date, it.period, it.name) }
            .sortedByDescending { it.date }.take(3000)
        val json = JSONArray().apply { next.forEach { put(JSONObject().put("name", it.name).put("date", it.date).put("period", it.period)) } }
        val stream = file.startWrite()
        try { stream.write(json.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
        SchoolSyncState.dataRevision.value++
    }
}

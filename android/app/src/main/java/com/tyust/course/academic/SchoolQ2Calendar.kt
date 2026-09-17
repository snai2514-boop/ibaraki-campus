package com.tyust.course.academic

import java.text.SimpleDateFormat
import java.util.*

data class SchoolCalendarEvent(val date: String, val lesson: PortalLesson, val examPossible: Boolean)

/** Official 2026 engineering timetable PDF, printed pages 1 and 17. Q2 slice only. */
object SchoolQ2Calendar {
    const val source = "https://www.eng.ibaraki.ac.jp/common/education/class/2026-subject05.pdf"
    fun events(item: PortalImport): List<SchoolCalendarEvent> {
        require(item.key.substringAfterLast('/') == "timetable-2026-Q2") { "目前仅核对了 2026 Q2 校历" }
        return SchoolAcademicCalendar.events(item)
    }
    fun ics(item: PortalImport, profile: SchoolStudentProfile? = null): String {
        val events = SchoolAcademicCalendar.events(item, profile)
        val term = item.key.substringAfterLast('/').removePrefix("timetable-")
        fun escape(value: String) = value.replace("\\", "\\\\").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
        val utc = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }
        fun instant(date: String, time: String): String {
            val cal = IbarakiTimetable.parseDate(date) ?: error("日期无效")
            cal.set(Calendar.HOUR_OF_DAY, time.substringBefore(':').toInt()); cal.set(Calendar.MINUTE, time.substringAfter(':').toInt())
            return utc.format(cal.time)
        }
        val lines = mutableListOf("BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Ibaraki Campus Assistant//School Calendar//ZH", "CALSCALE:GREGORIAN", "X-WR-CALNAME:$term 学校课表", "X-WR-TIMEZONE:Asia/Tokyo")
        val stamp = utc.format(Date())
        events.forEach { e ->
            val times = IbarakiTimetable.periodTimes.getValue(e.lesson.period)
            val identity = "${item.key}/${e.date}/${e.lesson.day}/${e.lesson.period}/${e.lesson.description.substringBefore(' ')}"
            val id = UUID.nameUUIDFromBytes(identity.toByteArray(Charsets.UTF_8))
            lines += listOf("BEGIN:VEVENT", "UID:$id@ibaraki-campus.local", "DTSTAMP:$stamp",
                "DTSTART:${instant(e.date, times.first)}", "DTEND:${instant(e.date, times.second)}",
                "SUMMARY:${escape(e.lesson.description)}",
                "DESCRIPTION:${escape("学校对应校区校历计划，非出勤记录。仅 $term 日期范围；不包含预备日或临时调课。" + if (e.examPossible) " 本次为考试可能日，具体安排以教师通知为准。" else "")}",
                "END:VEVENT")
        }
        lines += "END:VCALENDAR"
        // Fold UTF-8 lines without splitting a Unicode code point.
        return lines.joinToString("\r\n", postfix = "\r\n") { line ->
            val out = StringBuilder(); var bytes = 0; var i = 0
            while (i < line.length) {
                val cp = line.codePointAt(i); val s = String(Character.toChars(cp)); val n = s.toByteArray(Charsets.UTF_8).size
                if (bytes + n > 75) { out.append("\r\n "); bytes = 1 }
                out.append(s); bytes += n; i += Character.charCount(cp)
            }
            out.toString()
        }
    }
}

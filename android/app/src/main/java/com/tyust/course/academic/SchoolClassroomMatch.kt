package com.tyust.course.academic

data class SchoolClassroomReading(val code: String, val date: String, val period: Int, val room: String)
object SchoolClassroomMatch {
    fun key(snapshotKey: String, code: String, date: String, period: Int) = "$snapshotKey/$code/$date/$period"
    fun updates(owner: String, snapshots: List<PortalImport>, readings: List<SchoolClassroomReading>, profile: SchoolStudentProfile? = null): Map<String, String> {
        if(owner.isBlank()) return emptyMap()
        val candidates = mutableListOf<Pair<String, String>>()
        for (snapshot in snapshots.filter { it.key.startsWith("$owner/timetable-") }) {
            val events = SchoolAcademicCalendar.resolve(snapshot, profile).events
            for (r in readings) {
                if(r.room.isBlank() || r.room.length>80 || IbarakiTimetable.parseDate(r.date)==null) continue
                if(events.any { it.date==r.date && it.lesson.period==r.period && it.lesson.description.substringBefore(' ')==r.code })
                    candidates += key(snapshot.key,r.code,r.date,r.period) to r.room
            }
        }
        return candidates.groupBy({it.first},{it.second}).mapNotNull { (key, rooms) -> rooms.distinct().singleOrNull()?.let { key to it } }.toMap()
    }
}

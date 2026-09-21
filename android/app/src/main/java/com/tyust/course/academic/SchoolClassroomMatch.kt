package com.tyust.course.academic

data class SchoolClassroomReading(val code: String, val date: String, val period: Int, val room: String, val name: String = "")
object SchoolClassroomMatch {
    fun key(snapshotKey: String, code: String, date: String, period: Int) = "$snapshotKey/$code/$date/$period"
    fun updates(owner: String, snapshots: List<PortalImport>, readings: List<SchoolClassroomReading>, profile: SchoolStudentProfile? = null): Map<String, String> {
        if(owner.isBlank()) return emptyMap()
        val candidates = mutableListOf<Pair<String, String>>()
        val scoped = snapshots.filter { it.key.startsWith("$owner/timetable-") }
            .associateWith { SchoolAcademicCalendar.resolve(it, profile).events }
        for (r in readings) {
                if(r.room.isBlank() || r.room.length>80 || IbarakiTimetable.parseDate(r.date)==null) continue
                val matches = scoped.flatMap { (snapshot, events) -> events.filter {
                    it.date == r.date && it.lesson.period == r.period &&
                        if (r.code.isNotBlank()) it.lesson.description.substringBefore(' ') == r.code
                        else SchoolLiveCalendar.matchesName(it.lesson.description, r.name)
                }.map { key(snapshot.key, it.lesson.description.substringBefore(' '), r.date, r.period) } }.distinct()
                // Never choose arbitrarily between courses with the same name and slot.
                if (matches.size == 1) candidates += matches.single() to r.room
        }
        return candidates.groupBy({it.first},{it.second}).mapNotNull { (key, rooms) -> rooms.distinct().singleOrNull()?.let { key to it } }.toMap()
    }
}

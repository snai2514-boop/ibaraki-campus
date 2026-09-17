package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolClassroomMatchTest {
    private val snapshot = PortalImport("owner/timetable-2026-Q2", "", emptyList(), lessons = listOf(PortalLesson(1,3,"T5001 線形代数Ⅰ【情報】 2.0単位")))
    @Test fun acceptsOnlyExactAccountDateCourseAndPeriod() {
        val valid = SchoolClassroomReading("T5001","2026-06-15",3,"共通21")
        assertEquals(1, SchoolClassroomMatch.updates("owner",listOf(snapshot),listOf(valid)).size)
        listOf(valid.copy(code="T5008"),valid.copy(period=2),valid.copy(date="2026-08-15"),valid.copy(room="")).forEach {
            assertTrue(SchoolClassroomMatch.updates("owner",listOf(snapshot),listOf(it)).isEmpty())
        }
        assertTrue(SchoolClassroomMatch.updates("another",listOf(snapshot),listOf(valid)).isEmpty())
        assertTrue(SchoolClassroomMatch.updates("owner",listOf(snapshot),emptyList()).isEmpty())
        assertTrue(SchoolClassroomMatch.updates("owner",listOf(snapshot),listOf(valid,valid.copy(room="共通22"))).isEmpty())
    }
    @Test fun changingRoomsSplitWeekCardsInsteadOfLeakingAcrossDates() {
        val events = SchoolAcademicCalendar.events(snapshot)
        val model = SchoolTermView(2, events) { if(it.date=="2026-06-15") "共通21" else "教室待确认" }
        val known = model.courses.single { it.location=="共通21" }
        assertEquals("3", known.weeks)
        assertFalse(model.courses.single { it.location=="教室待确认" }.weeks.split(',').contains("3"))
    }
}

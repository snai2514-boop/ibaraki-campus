package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class SchoolDeliveryTest {
    @Test fun screenshotLiveOnlineFieldOverridesUnknownClassroomForAnyCode() {
        assertEquals("线上授课（实时）",SchoolCourseVenue.label("owner/timetable-2026-Q3","KB9999 表現行動と心の健康","","オンライン授業（リアルタイム配信型） ／on-line course (real time)"))
    }
    @Test fun allMethodsStayDistinct() {
        assertEquals("线上授课（录播）",SchoolCourseVenue.deliveryLabel("オンライン授業（オンデマンド型）"))
        assertEquals("线下授课",SchoolCourseVenue.deliveryLabel("対面授業 / face-to-face course"))
        assertEquals("混合授课",SchoolCourseVenue.deliveryLabel("ブレンド型授業 / blended course"))
        assertEquals("混合授课",SchoolCourseVenue.deliveryLabel("対面・オンライン併用"))
        assertNull(SchoolCourseVenue.deliveryLabel(""))
    }
    @Test fun conditionalCohortExceptionsAndMissingDataArePreserved() {
        assertEquals("水戸101",SchoolCourseVenue.label("timetable-2026-Q1","KB4012 情報","水戸101","オンライン授業"))
        assertEquals("教室待确认",SchoolCourseVenue.label("timetable-2026-Q3","UNKNOWN Course"))
        assertEquals("混合授课 · 101",SchoolCourseVenue.label("timetable-2026-Q3","OTHER Course","101","blended course"))
    }
    @Test fun publishedRoomNeverOverridesVerifiedOnlineDeliveryAcrossYears() {
        for (year in listOf(2026, 2027, 2028)) {
            assertEquals("线上授课（实时）", SchoolCourseVenue.label("owner/timetable-$year-calendar", "KB9999 Course", "共通教育棟2号館21番教室", "オンライン授業（リアルタイム配信型）"))
        }
    }
}

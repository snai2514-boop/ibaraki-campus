package com.tyust.course.academic
import org.junit.Assert.*
import org.junit.Test
class SchoolHolidaysTest {
 @Test fun summerIncludesBothEndsButNotTeachingOrReserveDays() {
  assertEquals("暑假",SchoolHolidays.on("2026-09-16")?.name)
  assertEquals("暑假",SchoolHolidays.on("2026-08-12")?.name)
  assertEquals("暑假",SchoolHolidays.on("2026-09-20")?.name)
  assertNull(SchoolHolidays.on("2026-08-11"))
  assertNull(SchoolHolidays.on("2026-09-21"))
 }
 @Test fun winterCrossesCalendarYear() {
  assertEquals("寒假",SchoolHolidays.on("2027-01-05")?.name)
  assertNull(SchoolHolidays.on("2027-01-06"))
  assertEquals("春假",SchoolHolidays.on("2027-03-31")?.name)
 }
 @Test fun unknownYearsAndEmptyTeachingDaysAreNotAssumedHolidays() {
  assertNull(SchoolHolidays.on("2028-09-16"))
  assertNull(SchoolHolidays.on("2026-06-01"))
 }
 @Test fun classroomHolidayNoticeUsesPublishedBoundariesOnly() {
  for (holiday in SchoolHolidays.breaks) {
   assertNotNull(SchoolHolidays.classroomNotice(holiday.start))
   assertNotNull(SchoolHolidays.classroomNotice(holiday.end))
   assertTrue(SchoolHolidays.classroomReason(holiday.start)!!.contains(holiday.start))
   assertTrue(SchoolHolidays.classroomReason(holiday.end)!!.contains(holiday.name))
  }
  for (date in listOf("2026-09-21", "2026-08-11", "2027-01-06", "2028-08-20")) {
   assertNull(SchoolHolidays.classroomNotice(date))
   assertNull(SchoolHolidays.classroomReason(date))
  }
 }
}

package com.tyust.course.academic

import com.tyust.course.i18n.LanguageDefaults
import com.tyust.course.i18n.TranslationCatalog
import org.junit.Assert.*
import org.junit.Test

class LanguageTest {
    private val catalog = TranslationCatalog(mapOf(
        "日历" to mapOf("en" to "Calendar", "ja" to "カレンダー", "ko" to "캘린더"),
        "第 {0} 周" to mapOf("en" to "Week {0}", "ja" to "第{0}週", "ko" to "{0}주차"),
        "学号：{0}" to mapOf("en" to "Student ID: {0}"),
        "预计总分：{0} + {1} = {2} 学分" to mapOf("en" to "Total: {0} + {1} = {2} credits")
    ))
    @Test fun defaultsPreserveExistingChineseButNewInstallIsJapanese() {
        assertEquals("ja", LanguageDefaults.select(null, false))
        assertEquals("zh", LanguageDefaults.select(null, true))
        listOf("zh", "en", "ko", "ja").forEach { assertEquals(it, LanguageDefaults.select(it, true)) }
    }
    @Test fun everyLanguageAndChineseFallback() {
        assertEquals("Calendar", catalog.translate("日历", "en"))
        assertEquals("カレンダー", catalog.translate("日历", "ja"))
        assertEquals("캘린더", catalog.translate("日历", "ko"))
        assertEquals("日历", catalog.translate("日历", "zh"))
    }
    @Test fun placeholdersKeepValuesWithoutReplacementInterpretation() {
        assertEquals("Week 12", catalog.translate("第 12 周", "en"))
        assertEquals("Student ID: a\$1\\b", catalog.translate("学号：a\$1\\b", "en"))
        assertEquals("Total: 25 + 6 = 31 credits", catalog.translate("预计总分：25 + 6 = 31 学分", "en"))
    }
    @Test fun schoolAndUserTextAreNotRewritten() {
        assertEquals("情報工学科", catalog.translate("情報工学科", "en"))
        assertEquals("My 日历 project", catalog.translate("My 日历 project", "en"))
    }
    @Test fun multiLineUiIsTranslatedLineByLine() {
        assertEquals("Calendar\nWeek 2", catalog.translate("日历\n第 2 周", "en"))
    }
}

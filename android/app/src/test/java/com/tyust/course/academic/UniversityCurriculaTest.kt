package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class UniversityCurriculaTest {
    @Test fun cohortComesFromSchoolRequirementDateNotStudentNumberOrGrade() {
        for(year in 1..4) {
            val p = SchoolStudentProfileParser.parse("学生番号 | 99T0000X | 学生氏名 | TEST\n所属 | 工学部情報工学科 | 学年 | ${year}年\n要件年月 | 2026年04月 | 入学年月日 | 2024年4月1日")!!
            assertEquals(2026, UniversityCurricula.scope(p).cohort)
            assertEquals(2024, p.admissionYear)
            assertTrue(UniversityCurricula.summary(UniversityCurricula.scope(p))!!.detailedInformation2026)
        }
        val p = SchoolStudentProfileParser.parse("学生番号 | 26T0000X | 学生氏名 | TEST\n所属 | 工学部情報工学科 | 学年 | 1年")!!
        assertNull(UniversityCurricula.summary(UniversityCurricula.scope(p)))
    }
    @Test fun unknownAndExcludedCohortsNeverReuse2026() {
        listOf(null,2023,2027).forEach { assertNull(UniversityCurricula.summary(CurriculumScope("工学部","情報工学科",it))) }
        assertNull(UniversityCurricula.summary(CurriculumScope("工学部","unknown",2026)))
        assertNull(UniversityCurricula.summary(CurriculumScope("都市学部","情報工学科",2026)))
    }
    @Test fun civilAndArchitectureRequireExplicitProgramAndHaveDifferentMinimums() {
        val base = CurriculumScope("工学部","都市システム工学科",2026)
        assertNull(UniversityCurricula.summary(base))
        assertEquals(54, UniversityCurricula.summary(base.copy(program="社会基盤デザインプログラム"))!!.rows.toMap()["専門必修"])
        assertEquals(69, UniversityCurricula.summary(base.copy(program="建築デザインプログラム"))!!.rows.toMap()["専門必修"])
    }
    @Test fun facultySpecificMinimumsAndSpecialProgramsAreSeparate() {
        assertEquals(22, UniversityCurricula.summary(CurriculumScope("人文社会科学部","現代社会学科",2026))!!.rows.toMap()["基盤教育科目"])
        assertEquals(25, UniversityCurricula.summary(CurriculumScope("人文社会科学部","人間文化学科",2024))!!.rows.toMap()["自由履修"])
        assertEquals(5, UniversityCurricula.summary(CurriculumScope("理学部","理学科",2025,"地球環境科学コース 地球科学技術者養成プログラム"))!!.rows.toMap()["自由履修"])
        val edu = CurriculumScope("教育学部","学校教育教員養成課程",2026,"教科教育コース 国語選修 Aタイプ")
        assertEquals(79, UniversityCurricula.summary(edu)!!.rows.toMap()["専門科目"])
        assertEquals(83, UniversityCurricula.summary(edu.copy(program="教科教育コース 国語選修 Bタイプ"))!!.rows.toMap()["専門科目"])
        assertNull(UniversityCurricula.summary(edu.copy(program="教科教育コース 国語選修")))
    }
    @Test fun agricultureMinimaDoNotReplaceOverallTotalAndCohortsDiffer() {
        val old = CurriculumScope("農学部", "地域総合農学科", 2024, "地域共生コース 工学系")
        val current = CurriculumScope("農学部", "地域総合農学科", 2026, "地域共生コース")
        assertEquals(86, UniversityCurricula.summary(old)!!.rows.toMap()["専門科目"])
        val rule = UniversityCurricula.summary(current)!!
        assertEquals(90, rule.rows.toMap()["専門科目"])
        assertEquals(124, rule.total)
        assertTrue(rule.rows.sumOf { it.second } < rule.total)
        assertTrue(rule.source.endsWith("2026freshman2.pdf"))
    }
    @Test fun allInScopeFacultiesHaveCohortSpecificSources() {
        for(cohort in 2024..2026) {
            val examples = listOf(
                CurriculumScope("工学部", "情報工学科", cohort),
                CurriculumScope("人文社会科学部", "現代社会学科", cohort),
                CurriculumScope("理学部", "理学科", cohort, "物理学コース"),
                CurriculumScope("教育学部", "養護教諭養成課程", cohort),
                CurriculumScope("農学部", "地域総合農学科", cohort, if(cohort == 2024) "農業科学コース" else "応用植物科学コース"),
                CurriculumScope("地域未来共創学環", "地域未来共創学環", cohort))
            examples.forEach { assertNotNull(it.toString(), UniversityCurricula.summary(it)) }
        }
    }
    @Test fun schoolYearAndGakkanSupportedWithoutAssumingDepartment() {
        val p = SchoolStudentProfileParser.parse("学生番号 | 24X0000X | 学生氏名 | TEST\n所属 | 地域未来共創学環 | 学年 | 3年\n要件年月 | 2024年04月 | 入学年月日 | 2024年4月1日")!!
        assertEquals("地域未来共創学環",p.faculty)
        assertEquals("地域未来共創学環",UniversityCurricula.scope(p).department)
        assertTrue(UniversityCurricula.profileMatches(p,UniversityCurricula.owner(p)+"/grades"))
        assertFalse(UniversityCurricula.profileMatches(p,"wrong/grades"))
    }
}

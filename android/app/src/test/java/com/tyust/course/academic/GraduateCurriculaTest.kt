package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class GraduateCurriculaTest {
    private fun scope(f: String,d: String,p: String="",l: StudyLevel=StudyLevel.MASTER,t: String="",y:Int=2026) = CurriculumScope(f,d,y,p,l,t)
    @Test fun everyPublishedGraduateMajorAndTrackHasScopedRules() {
        var count=0
        GraduateCurricula.departments.keys.forEach { f ->
            GraduateCurricula.levels(f).filter { it !in setOf(StudyLevel.UNKNOWN,StudyLevel.NON_DEGREE) }.forEach { level ->
                GraduateCurricula.majors(f,level).forEach { d ->
                    val base=scope(f,d,l=level)
                    GraduateCurricula.programs(base).ifEmpty { listOf("") }.forEach { p ->
                        val current=base.copy(program=p)
                        GraduateCurricula.tracks(current).ifEmpty { listOf("") }.forEach { track ->
                            val s=current.copy(track=track); val r=GraduateCurricula.rule(s)
                            assertNotNull(s.toString(),r)
                            assertNull(UniversityCurricula.summary(s))
                            assertTrue(r!!.conditions.isNotEmpty())
                            assertTrue(GraduateCurricula.leaves(r).map { it.id }.distinct().size == GraduateCurricula.leaves(r).size)
                            assertFalse(GraduateCurricula.progress(s,emptyList()).categories.single().satisfied)
                            count++
                        }
                    }
                }
            }
        }
        assertTrue(count>=50)
    }
    @Test fun agriculturePreservesCoreAndOtherModuleMinimumsFromPhoto() {
        val s=scope(GraduateCurricula.AGRICULTURE,"農学専攻","実践農食科学コース")
        val r=GraduateCurricula.rule(s)!!
        assertEquals(30,r.total); assertEquals(2,r.standardYears)
        assertEquals(listOf("2","3","4","3","2","14"),r.requirements.map { it.required.toPlainString() })
        val grades=listOf(PortalGrade("A",7.toBigDecimal(),80,"A",true,"2026","前期","コアモジュール科目"))
        val progress=GraduateCurricula.progress(s,grades).categories.single()
        assertEquals(0,progress.children.single { it.requirement.id=="ownOther" }.earned.signum())
    }
    @Test fun professionalPsychologyAndDoctoralTotalsDiffer() {
        assertEquals(38,GraduateCurricula.rule(scope(GraduateCurricula.HUMAN,"人文科学専攻","公認心理師コース",t="一般専門教育"))!!.total)
        assertEquals(48,GraduateCurricula.rule(scope(GraduateCurricula.EDUCATION,"教育実践高度化専攻","教科領域コース",StudyLevel.PROFESSIONAL,"現職教員"))!!.total)
        assertEquals(14,GraduateCurricula.rule(scope(GraduateCurricula.SCIENCE,"量子線科学専攻",l=StudyLevel.DOCTOR))!!.total)
        val joint=GraduateCurricula.rule(scope(GraduateCurricula.UNITED,"生物生産科学専攻","一般",StudyLevel.DOCTOR))!!
        assertEquals(12,joint.total); assertEquals("9.5",joint.requirements.first().required.toPlainString())
        assertTrue(joint.notes.contains("东京农工大学"))
    }
    @Test fun uncertainIdentityYearsAndInvalidCombinationsNeverGetFallbackRules() {
        val s=scope(GraduateCurricula.AGRICULTURE,"農学専攻","応用植物科学コース")
        listOf(s.copy(cohort=2027),s.copy(cohort=null),s.copy(level=StudyLevel.NON_DEGREE),s.copy(level=StudyLevel.UNKNOWN),s.copy(level=StudyLevel.DOCTOR),s.copy(program="未知方向")).forEach {
            assertNull(GraduateCurricula.rule(it)); assertNull(UniversityCurricula.summary(it))
        }
        assertNull(GraduateCurricula.rule(scope(GraduateCurricula.HUMAN,"人文科学専攻","公認心理師コース",t="留学生専門教育")))
    }
    @Test fun profileRecognizesGraduateAffiliationWithoutStudentNumberGuesses() {
        val p=SchoolStudentProfileParser.parse("学生番号 | 26AM000X | 学生氏名 | TEST\n所属 | 茨城大学大学院 農学研究科 修士課程 農学専攻 応用植物科学コース | 学年 | 1年\n要件年月 | 2026年04月")!!
        val s=UniversityCurricula.scope(p)
        assertEquals(GraduateCurricula.AGRICULTURE,p.faculty);assertEquals("農学専攻",s.department)
        assertEquals(StudyLevel.MASTER,s.level);assertEquals("応用植物科学コース",s.program)
        assertNotNull(GraduateCurricula.rule(s))
        assertEquals(StudyLevel.NON_DEGREE,UniversityCurricula.scope(p.copy(admissionType="研究生")).level)
        assertEquals(StudyLevel.UNKNOWN,GraduateCurricula.detectLevel("農学研究科 農学専攻"))
        assertEquals(StudyLevel.UNDERGRADUATE,GraduateCurricula.detectLevel("農学部 食生命科学科"))
    }
    @Test fun outsideFailedAndUnknownCreditsDoNotSatisfyGraduateRequirements() {
        val s=scope(GraduateCurricula.HUMAN,"社会科学専攻","法学・行政学コース",t="一般専門教育")
        fun g(name:String,cat:String,ok:Boolean=true)=PortalGrade(name,2.toBigDecimal(),null,"認",ok,"2026","前期",cat)
        val r=GraduateCurricula.progress(s,listOf(g("日本語表現法",""),g("研究法","研究指導科目",false),g("不明",""),g("正式課程外","修了要件外科目")))
        assertEquals(0,r.categories.single().earned.signum());assertEquals(0,r.unassigned.compareTo(2.toBigDecimal()))
        // An undergraduate general-education title must not trigger undergraduate classification.
        assertNull(GraduateCurricula.classify(s,g("大学入門ゼミ","")).categoryId)
    }
}

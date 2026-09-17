package com.tyust.course.academic

import java.io.File
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class UniversityCourseClassificationTest {
    private val catalog by lazy { UniversityCourseCatalog.parse(File("src/main/assets/course-categories.tsv").readText()) }
    private val info = CurriculumScope("工学部", "情報工学科", 2026)
    private fun category(s: CurriculumScope, name: String, school: String = "") = UniversityCourseClassification.classify(s, name, school, catalog)
    private fun grade(name: String, category: String = "", passed: Boolean = true) = PortalGrade(name, BigDecimal("2"), 80, "A", passed, "2026", "後期", category)
    private fun snapshot(vararg grades: PortalGrade) = PortalImport("account/grades", "grades", emptyList(), grades.toList())

    @Test fun screenshotCoursesAndAllGeneralSubjectsResolve() {
        mapOf("身体活動" to "health", "経済・経営" to "society", "共生とコミュニケーション" to "culture_communication",
            "物質と生命" to "nature", "Integrated English １Ａ" to "english", "データサイエンス・AI入門" to "data_ai").forEach { (name,id) ->
            assertEquals(name,id,category(info,name).categoryId)
        }
        assertEquals("faculty_core",category(info,"線形代数Ⅰ").categoryId)
        assertEquals("department_core",category(info,"ソフトウェア基礎").categoryId)
        assertEquals("cross_program",category(info,"情報セキュリティ").categoryId)
        assertEquals("professional_elective",category(info,"数値解析").categoryId)
    }
    @Test fun allFacultiesAndCohortsHaveScopedCatalogEntries() {
        for (year in 2024..2026) for ((faculty,departments) in UniversityCurricula.departments) for (department in departments) {
            val scope = CurriculumScope(faculty,department,year)
            val programs = UniversityCurricula.programs(scope).ifEmpty { listOf("") }
            for (program in programs) {
                val s=scope.copy(program=program)
                val applicable=catalog.rules.filter { it.cohort==year && it.faculty==faculty && it.department==department && it.program in listOf("*",program) }
                assertTrue("No courses: $s",applicable.isNotEmpty())
                assertNotNull("Missing requirements: $s",UniversityCurricula.summary(s))
                assertTrue("No usable classification: $s",applicable.any { category(s,it.name).categoryId != null })
            }
        }
    }
    @Test fun programAndDepartmentBoundariesAreNotInterchangeable() {
        val materials=CurriculumScope("工学部","物質科学工学科",2026,"材料工学プログラム")
        assertEquals("専門必修",category(materials,"材料強度学").label)
        assertEquals("専門選択必修",category(materials.copy(program="化学・生命工学プログラム"),"材料強度学").label)
        assertNull(category(info,"材料強度学").categoryId)
        val english=CurriculumScope("教育学部","学校教育教員養成課程",2026,"教科教育コース 英語選修 Aタイプ")
        assertEquals("専門科目",category(english,"英語学概論A").label)
        assertEquals("自由履修",category(english.copy(program="教科教育コース 数学選修 Aタイプ"),"英語学概論A").label)
        assertEquals("学部基礎科目",category(CurriculumScope("人文社会科学部","現代社会学科",2024),"メディア文化入門").label)
        assertEquals("専門科目",category(CurriculumScope("農学部","食生命科学科",2026),"食品化学").label)
    }
    @Test fun mechanicalProgramSeparatesCoreAndElectiveCourses() {
        val m=CurriculumScope("工学部","機械システム工学科",2026,"生産システムプログラム")
        assertEquals("専門選択必修",category(m,"機械学習Ⅱ").label)
        assertEquals("専門選択必修",category(m.copy(program="制御システムプログラム"),"機械学習Ⅱ").label)
        assertEquals("専門必修",category(m,"機械設計工学").label)
        assertEquals("専門選択必修",category(m.copy(program="環境エネルギーシステムプログラム"),"機械設計工学").label)
    }
    @Test fun sourceClassificationWinsAndOutsideCreditsStayOutOfProgress() {
        assertTrue(category(info,"職業指導").excluded)
        assertEquals("free",category(info,"職業指導","自由履修").categoryId)
        val p=UniversityCourseClassification.progress(info,snapshot(grade("職業指導"),grade("線形代数Ⅰ"),grade("知らない科目"),grade("ソフトウェア基礎",passed=false)),catalog)
        assertEquals(0,BigDecimal("2").compareTo(p.categories.single().earned))
        assertEquals(0,BigDecimal("2").compareTo(p.unassigned))
        assertNull(category(info.copy(cohort=2023),"線形代数Ⅰ").categoryId)
        assertNull(category(info.copy(faculty="不明"),"身体活動").categoryId)
    }
    @Test fun forecastKeepsRepeatedOfferingsDistinctAndSameResolverForPendingRows() {
        val courses=listOf(ForecastCourse("SAME","経済・経営",BigDecimal("2"),3,"预计新增"),ForecastCourse("SAME","経済・経営",BigDecimal("2"),4,"预计新增"),ForecastCourse("OTHER","未知",BigDecimal.ONE,4,"预计新增"))
        val f=UniversityCourseClassification.forecast(info,CreditForecast(courses,BigDecimal.ZERO,emptyList()),snapshot(),catalog)
        assertEquals(0,BigDecimal("4").compareTo(f.categories.single().earned))
        assertEquals(BigDecimal.ONE,f.unassigned)
        assertNull(UniversityCourseClassification.classifyForecast(info,courses.last(),snapshot(),catalog).categoryId)
    }
    @Test fun conflictingCatalogMappingsStayPendingAndRomanNumeralsStayDistinct() {
        val c=UniversityCourseCatalog(listOf("department_core","professional_elective").map { CourseCategoryRule(2026,"工学部","情報工学科","*","科目Ⅰ",it,"https://example.test/",1) })
        assertNull(UniversityCourseClassification.classify(info,"科目I",catalog=c).categoryId)
        assertTrue(c.find(info,"科目II").isEmpty())
        assertTrue(c.find(info.copy(cohort=2025),"科目I").isEmpty())
    }
    @Test fun individualSchoolRecognitionOutranksGenericForecastCatalog() {
        val course=ForecastCourse("X","線形代数Ⅰ",BigDecimal("2"),4,"预计新增")
        assertEquals("free",UniversityCourseClassification.classifyForecast(info,course,snapshot(grade(course.name,"自由履修")),catalog).categoryId)
        assertNull(UniversityCourseClassification.classifyForecast(info,course,snapshot(grade(course.name,"自由履修"),grade(course.name,"学部共通専門基礎教育科目")),catalog).categoryId)
    }
    @Test fun secondSemesterEnglishCountsInEnglishPreviewAcrossFaculties() {
        for (year in 2024..2026) for ((faculty, departments) in UniversityCurricula.departments) for (department in departments) {
            val base = CurriculumScope(faculty, department, year)
            for (program in UniversityCurricula.programs(base).ifEmpty { listOf("") }) {
              val scope = base.copy(program = program)
              for (level in 1..3) for (suffix in listOf("A", "B", "C", "D")) {
                assertNotNull("$scope English $level$suffix", category(scope, "Integrated English $level$suffix").categoryId)
              }
            }
        }
        val courses = listOf("Integrated English １Ｃ", "Integrated English 3D").mapIndexed { i, name ->
            ForecastCourse("ENGLISH$i", name, BigDecimal.ONE, 4, "预计新增")
        }
        val earned = snapshot(grade("Integrated English 1A"))
        val forecast = UniversityCourseClassification.forecast(info, CreditForecast(courses, BigDecimal("2"), emptyList()), earned, catalog)
        assertEquals(BigDecimal("2"), forecast.categories.single().children.single { it.requirement.id == "general" }.children.single { it.requirement.id == "foundation" }.children.single { it.requirement.id == "english" }.earned)
        assertEquals(0, forecast.unassigned.compareTo(BigDecimal.ZERO))
        assertEquals(BigDecimal("2"), UniversityCourseClassification.progress(info, earned, catalog).categories.single().earned)
        assertNull(category(info, "Integrated English 4C").categoryId)
    }
    @Test fun officialCodesAndCreditsResolveRealEducationHomonyms() {
        val scope=CurriculumScope("教育学部","学校教育教員養成課程",2026,"教科教育コース 英語選修 Aタイプ")
        val name="特別支援教育実地研究"
        fun resolve(code: String, credits: String) = UniversityCourseClassification.classify(scope,name,catalog=catalog,code=code,credits=credits.toBigDecimal())
        assertNull(resolve("P3011","5").categoryId)
        assertEquals("専門科目",resolve("P3012","3").label)
        assertEquals("専門科目",resolve("","3").label)
        assertNull(resolve("","5").categoryId)
        assertNull(resolve("P3011","3").categoryId)
        assertNull(resolve("P9999","3").categoryId)
        assertEquals("専門科目",category(scope,name).label)
        val special = scope.copy(program = "特別支援教育コース")
        assertEquals("専門科目", UniversityCourseClassification.classify(special,name,catalog=catalog,code="P3011",credits=BigDecimal("5")).label)
        assertNull(UniversityCourseClassification.classify(special,name,catalog=catalog,code="P3012",credits=BigDecimal("3")).categoryId)
    }

    @Test fun auditEveryCatalogCourseInEveryApplicableProgram() {
        val unresolved = linkedSetOf<String>()
        var checked = 0
        for (year in 2024..2026) for ((faculty, departments) in UniversityCurricula.departments) for (department in departments) {
            val base = CurriculumScope(faculty, department, year)
            for (program in UniversityCurricula.programs(base).ifEmpty { listOf("") }) {
                val scope = base.copy(program = program)
                catalog.rules.filter { it.cohort == year && it.faculty == faculty && it.department == department && it.program in listOf("*", program) }.forEach { row ->
                    checked++
                    val result = UniversityCourseClassification.classify(scope, row.name, catalog = catalog, code = row.code, credits = row.credits)
                    if (result.categoryId == null && !result.excluded) unresolved.add(listOf(year, faculty, department, program, row.name, row.code, row.credits, row.category).joinToString("\t"))
                }
            }
        }
        File("build/reports/course-classification-audit.tsv").apply { parentFile.mkdirs(); writeText("# checked=$checked unresolved=${unresolved.size}\n" + unresolved.joinToString("\n")) }
        assertTrue(checked > 50000)
        assertTrue("Unresolved catalog rows: ${unresolved.take(10)}", unresolved.isEmpty())
    }
}

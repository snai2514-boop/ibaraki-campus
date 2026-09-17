package com.tyust.course.academic

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File

/** Export public rules from the Android implementation. Never reads a school account. */
class ExportAppleParityTest {
    @Test fun exportPublicRulesWhenRequested() {
        val destination = System.getenv("CAMPUS_EXPORT_PARITY") ?: return
        fun node(r: CreditRequirement): JSONObject = JSONObject().put("id", r.id).put("label", r.label)
            .put("required", r.required).put("children", JSONArray(r.children.map(::node)))
        val entries = JSONArray()
        val cases = JSONArray()
        val catalog = UniversityCourseCatalog.parse(File("src/main/assets/course-categories.tsv").readText())
        for (cohort in 2024..2026) for ((faculty, departments) in UniversityCurricula.departments) {
            for (department in departments) {
                val base = CurriculumScope(faculty, department, cohort)
                for (program in (listOf("") + UniversityCurricula.programs(base)).distinct()) {
                    val scope = base.copy(program = program)
                    val summary = UniversityCurricula.summary(scope)
                    val sample = catalog.rules.filter { it.cohort == cohort && it.faculty == faculty && it.department in listOf("*", department) && it.program in listOf("*", program) }
                        .distinctBy { it.category }.take(8)
                    for (row in sample) {
                        val result = UniversityCourseClassification.classify(scope, row.name, catalog = catalog, code = row.code, credits = row.credits)
                        cases.put(JSONObject().put("scope", JSONObject().put("faculty",faculty).put("department",department).put("cohort",cohort).put("program",program))
                            .put("course", JSONObject().put("name",row.name).put("code",row.code).put("credits",row.credits ?: JSONObject.NULL))
                            .put("expected",JSONObject().put("id",result.categoryId ?: JSONObject.NULL).put("label",result.label).put("excluded",result.excluded)))
                    }
                    entries.put(JSONObject().put("faculty", faculty).put("department", department)
                        .put("cohort", cohort).put("program", program)
                        .put("guide", summary?.source ?: UniversityCurricula.guide(scope))
                        .put("detail", summary?.detail ?: "毕业要求待核对")
                        .put("page", summary?.page ?: 0)
                        .put("detailed", summary?.detailedInformation2026 ?: false)
                        .put("checks", JSONArray(CurriculumConstraintChecks.specs(scope).map { c -> JSONObject()
                            .put("id",c.id).put("title",c.title).put("minimum",c.minimum)
                            .put("categories",JSONArray(c.categories)).put("names",JSONArray(c.names)) }))
                        .put("constraints", JSONArray(CurriculumConstraints.rules(scope).map { r -> JSONObject()
                            .put("id",r.id).put("title",r.title).put("scope",r.scope).put("detail",r.detail)
                            .put("page",r.page).put("source",r.source).put("limit",r.limit ?: JSONObject.NULL).put("unit",r.unit) }))
                        .put("requirements", JSONArray(UniversityCourseClassification.requirements(scope).map(::node))))
                }
            }
        }
        val generalField = UniversityCourseClassification::class.java.getDeclaredField("general").apply { isAccessible = true }
        val general = generalField.get(UniversityCourseClassification) as Map<*, *>
        val result = JSONObject().put("curricula", entries).put("general", JSONObject(general))
            .put("enrollmentRules", JSONArray(EnrollmentRules.all.map { r -> JSONObject()
                .put("id",r.id).put("title",r.title).put("scope",r.scope).put("detail",r.detail)
                .put("page",r.page).put("limit",r.limit ?: JSONObject.NULL).put("unit",r.unit) }))
            .put("enrollmentSource", EnrollmentRules.source)
        File(destination).apply { parentFile.mkdirs() }.writeText(result.toString(), Charsets.UTF_8)
        File(File(destination).parentFile,"classification-parity.json").writeText(cases.toString(), Charsets.UTF_8)
    }
}

package com.tyust.course.academic

import android.content.Context
import android.util.AtomicFile
import java.io.File
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow

data class SchoolStudentProfile(val name: String, val studentNumber: String, val faculty: String,
    val department: String, val year: String, val affiliation: String, val updatedAt: Long = System.currentTimeMillis(),
    val curriculumYear: Int? = null, val admissionYear: Int? = null, val program: String = "",
    val admissionType: String = "")

object SchoolStudentProfileParser {
    fun parse(text: String): SchoolStudentProfile? {
        val values = mutableMapOf<String, String>()
        for (line in text.lines()) {
            val cells = line.split(" | ").map(String::trim)
            if (cells.size % 2 != 0) continue
            for (i in cells.indices step 2) if (cells[i] in setOf("学生氏名", "学生番号", "所属", "学年", "要件年月", "入学年月日", "入学区分", "入学年次", "コース", "プログラム")) {
                if (values.containsKey(cells[i]) && values[cells[i]] != cells[i + 1]) return null
                values[cells[i]] = cells[i + 1]
            }
        }
        val number = values["学生番号"]?.takeIf { Regex("[A-Za-z0-9]{4,24}").matches(it) } ?: return null
        val name = values["学生氏名"]?.takeIf { it.isNotBlank() && it.length <= 100 } ?: return null
        val affiliation = values["所属"].orEmpty()
        val faculty = Regex("^(.+?(?:学部|学環))").find(affiliation)?.value.orEmpty()
        val department = if (faculty.isNotEmpty()) affiliation.removePrefix(faculty).trim() else ""
        fun dateYear(key: String) = values[key]?.let {
            Regex("^((?:19|20)\\d{2})(?:年|[-/])").find(java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFKC))?.groupValues?.get(1)?.toIntOrNull()
        }
        return SchoolStudentProfile(name, number, faculty, department, values["学年"].orEmpty(), affiliation,
            curriculumYear = dateYear("要件年月"), admissionYear = dateYear("入学年月日"),
            program = listOfNotNull(values["コース"], values["プログラム"]).filter { it.isNotBlank() }.distinct().joinToString(" / "),
            admissionType = values["入学区分"].orEmpty())
    }
}

class SchoolStudentProfileStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "school-student-profile-v1.json"))
    fun load(): SchoolStudentProfile? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        val j = JSONObject(file.openRead().bufferedReader().use { it.readText() })
        return SchoolStudentProfile(j.getString("name"), j.getString("studentNumber"), j.getString("faculty"),
            j.getString("department"), j.getString("year"), j.getString("affiliation"), j.getLong("updatedAt"),
            j.optInt("curriculumYear").takeIf { it in 1900..2099 }, j.optInt("admissionYear").takeIf { it in 1900..2099 },
            j.optString("program"), j.optString("admissionType"))
    }
    fun save(p: SchoolStudentProfile) {
        val j = JSONObject().put("name", p.name).put("studentNumber", p.studentNumber).put("faculty", p.faculty)
            .put("department", p.department).put("year", p.year).put("affiliation", p.affiliation).put("updatedAt", p.updatedAt)
            .put("curriculumYear", p.curriculumYear).put("admissionYear", p.admissionYear)
            .put("program", p.program).put("admissionType", p.admissionType)
        val stream = file.startWrite()
        try { stream.write(j.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
}

/** Process-local progress cannot remain stuck after process death. */
object SchoolSyncState {
    // Navigation access for this app process; never claims a live school session.
    val entered = MutableStateFlow(false)
    val running = MutableStateFlow(false)
    val message = MutableStateFlow("")
    val details = MutableStateFlow("")
    val dataRevision = MutableStateFlow(0L)
    val snapshots = MutableStateFlow<List<PortalImport>>(emptyList())
    val profile = MutableStateFlow<SchoolStudentProfile?>(null)
}

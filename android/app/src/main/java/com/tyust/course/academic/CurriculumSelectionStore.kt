package com.tyust.course.academic

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

class CurriculumSelectionStore(private val context: Context, private val owner: String) {
    init { require(Regex("[a-f0-9]{64}").matches(owner)) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "curriculum-$owner.json"))
    fun load(): CurriculumScope? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        val j = JSONObject(file.openRead().bufferedReader().use { it.readText() })
        val faculty = j.getString("faculty")
        val fallback = if(faculty.contains("研究科")) StudyLevel.UNKNOWN else StudyLevel.UNDERGRADUATE
        val level = StudyLevel.entries.find { it.name == j.optString("level") } ?: fallback
        return CurriculumScope(faculty, j.getString("department"), j.getInt("cohort"), j.optString("program"), level, j.optString("track"))
    }
    fun save(s: CurriculumScope) {
        require(s.cohort in 1900..2099 && s.department in (UniversityCurricula.departments + GraduateCurricula.departments)[s.faculty].orEmpty())
        val j = JSONObject().put("faculty", s.faculty).put("department", s.department).put("cohort", s.cohort).put("program", s.program)
            .put("level", s.level.name).put("track", s.track)
        val out = file.startWrite()
        try { out.write(j.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(out) }
        catch(e: Exception) { file.failWrite(out); throw e }
    }
    fun reset() = file.delete()
    private fun evidence(s: CurriculumScope): AtomicFile {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(s.toString().toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(context.noBackupFilesDir, "curriculum-evidence-$owner-$digest.pdf"))
    }
    fun hasEvidence(s: CurriculumScope) = evidence(s).baseFile.exists()
    /** Supporting evidence only: saving a PDF never activates unreviewed numeric rules. */
    fun saveEvidence(s: CurriculumScope, uri: Uri) {
        val target = evidence(s)
        val out = target.startWrite()
        try {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                val header = ByteArray(5)
                var filled = 0
                while(filled < 5) { val n = input.read(header, filled, 5-filled); require(n > 0); filled += n }
                require(header.contentEquals("%PDF-".toByteArray()))
                out.write(header)
                val buffer = ByteArray(8192); var total = 5
                while(true) { val n = input.read(buffer); if(n < 0) break; total += n; require(total <= 32 * 1024 * 1024); out.write(buffer, 0, n) }
            }
            target.finishWrite(out)
        } catch(e: Exception) { target.failWrite(out); throw e }
    }
}

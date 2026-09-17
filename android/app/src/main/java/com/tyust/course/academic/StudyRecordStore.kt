package com.tyust.course.academic

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Local user-entered study records, separate from demo and school account caches; excluded from backup. */
class StudyRecordStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "ibaraki-study-records-v1.json"))
    fun load(): List<StudyRecord> {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return emptyList()
        val root = JSONObject(file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        require(root.getInt("version") == 1) { "暂不支持此记录版本" }
        val array = root.getJSONArray("records")
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            StudyRecord(item.getString("id"), item.getString("name"), item.getString("credits").toBigDecimal(),
                if (item.isNull("score")) null else item.getInt("score"), StudyGradeKind.valueOf(item.getString("kind")),
                if (item.isNull("category")) null else item.getString("category"))
        }.also(IbarakiStudyCalculator::validate)
    }
    fun save(records: List<StudyRecord>) {
        IbarakiStudyCalculator.validate(records)
        val array = JSONArray()
        records.forEach { item -> array.put(JSONObject().put("id", item.courseId).put("name", item.name)
            .put("credits", item.credits.toPlainString()).put("score", item.score ?: JSONObject.NULL)
            .put("kind", item.kind.name).put("category", item.categoryId ?: JSONObject.NULL)) }
        val stream = file.startWrite()
        try {
            stream.write(JSONObject().put("version", 1).put("records", array).toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (e: Exception) { file.failWrite(stream); throw e }
    }
}

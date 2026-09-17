package com.tyust.course.academic

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Reads existing snapshots without changing their contents or account/quarter keys. */
class SchoolImportStore(context: Context) {
    companion object { private val accessLock = Any() }
    private val file = AtomicFile(File(context.noBackupFilesDir, "school-imports-v1.json"))
    fun load(): List<PortalImport> = synchronized(accessLock) { readSaved().also { SchoolSyncState.snapshots.value = it } }
    private fun readSaved(): List<PortalImport> {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return emptyList()
        val array = JSONArray(file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        return (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            val cards = row.getJSONArray("cards").let { c -> (0 until c.length()).map(c::getString) }
            val item = PortalImport(row.getString("key"), row.getString("title"), cards)
            if (row.optInt("schema", 1) == 1) SchoolSnapshotMigration.expand(item)
            else {
                require(row.getInt("schema") == 2)
                val g = row.getJSONArray("grades")
                val l = row.getJSONArray("lessons")
                item.copy(syncedAt = row.optLong("syncedAt", 0), grades = (0 until g.length()).map { index ->
                    val v = g.getJSONObject(index)
                    PortalGrade(v.getString("name"), v.getString("credits").toBigDecimal(), if (v.isNull("score")) null else v.getInt("score"),
                        v.getString("grade"), v.getBoolean("passed"), v.getString("year"), v.getString("term"), v.getString("category"))
                }, lessons = (0 until l.length()).map { index ->
                    val v = l.getJSONObject(index)
                    PortalLesson(v.getInt("day"), v.getInt("period"), v.getString("description"))
                })
            }
        }
    }
    fun save(item: PortalImport): List<PortalImport> = synchronized(accessLock) { writeSaved(item) }
    private fun writeSaved(item: PortalImport): List<PortalImport> {
        // Always reload: corrupt or unreadable history must never be silently overwritten.
        val next = load().filterNot { it.key == item.key } + item
        val array = JSONArray()
        next.forEach { row ->
            val grades = JSONArray()
            row.grades.forEach { g -> grades.put(JSONObject().put("name", g.name).put("credits", g.credits.toPlainString())
                .put("score", g.score ?: JSONObject.NULL).put("grade", g.grade).put("passed", g.passed)
                .put("year", g.year).put("term", g.term).put("category", g.category)) }
            val lessons = JSONArray()
            row.lessons.forEach { l -> lessons.put(JSONObject().put("day", l.day).put("period", l.period).put("description", l.description)) }
            array.put(JSONObject().put("key", row.key).put("title", row.title).put("cards", JSONArray(row.cards))
                .put("schema", 2).put("syncedAt", row.syncedAt).put("grades", grades).put("lessons", lessons))
        }
        val stream = file.startWrite()
        try { stream.write(array.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
        SchoolSyncState.snapshots.value = next
        SchoolSyncState.dataRevision.value += 1
        return next
    }
}

/** Strict conversion of the app's own v1 display format, never a general school-page parser. */
object SchoolSnapshotMigration {
    fun expand(item: PortalImport): PortalImport {
        if (item.key.substringAfterLast('/') == "grades") {
            val grades = item.cards.drop(1).map { card ->
                val lines = card.lines()
                require(lines.size == 4) { "旧成绩快照无法转换，请重新读取学校页面" }
                val parts = lines[1].split(" · ")
                val term = lines[2].split(" · ")
                require(parts.size == 4 && term.size == 2 && parts[3] in listOf("通过", "未通过"))
                val credits = parts[0].removeSuffix(" 学分").toBigDecimal()
                require(credits.signum() > 0)
                PortalGrade(lines[0], credits, parts[1].toIntOrNull(), parts[2], parts[3] == "通过", term[0], term[1], lines[3])
            }
            return item.copy(grades = grades)
        }
        if (item.key.substringAfterLast('/').startsWith("timetable-")) {
            val lessons = item.cards.drop(1).map { card ->
                val match = Regex("^星期([一二三四五六]) · 第 ([1-6]) 限\\n(.+)$").matchEntire(card)
                    ?: error("旧课表快照无法转换，请重新读取学校页面")
                PortalLesson("一二三四五六".indexOf(match.groupValues[1]) + 1, match.groupValues[2].toInt(), match.groupValues[3])
            }
            return item.copy(lessons = lessons)
        }
        return item
    }

    fun studyRecords(item: PortalImport): List<StudyRecord> = item.grades.mapIndexed { index, g ->
        StudyRecord("${item.key}/$index", g.name, g.credits, g.score,
            if (g.score != null) StudyGradeKind.SCORED else if (g.passed) StudyGradeKind.RECOGNIZED else StudyGradeKind.PENDING, SchoolGradeClassification.categoryId(g))
    }
}

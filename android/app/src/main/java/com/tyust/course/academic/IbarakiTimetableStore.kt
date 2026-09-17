package com.tyust.course.academic

import android.content.Context
import android.util.AtomicFile
import java.io.File

class IbarakiTimetableStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "ibaraki-timetable-v1.json"))
    fun load(): IbarakiTimetable {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return IbarakiTimetable()
        return IbarakiTimetableCodec.decode(file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
    }
    fun save(timetable: IbarakiTimetable) {
        val text = IbarakiTimetableCodec.encode(timetable)
        val stream = file.startWrite()
        try { stream.write(text.toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
}

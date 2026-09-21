package com.tyust.course.utils

import android.content.Context
import com.tyust.course.BuildConfig
import java.io.File
import java.util.concurrent.Executors

/** Bounded, app-private history. Callers supply only structural metadata, never school text or URLs. */
object SyncDiagnostics {
    private val writer = Executors.newSingleThreadExecutor()
    private var directory: File? = null

    fun initialize(context: Context) {
        directory = File(context.noBackupFilesDir, "sync-diagnostics")
        record("process_start", "version=${BuildConfig.VERSION_NAME} sdk=${android.os.Build.VERSION.SDK_INT}")
    }

    fun record(event: String, metadata: String = "") {
        val line = "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", java.util.Locale.ROOT).format(java.util.Date())} $event ${metadata.replace('\n', ' ').replace('\r', ' ').take(700)}\n"
        android.util.Log.i("SchoolSync", line.trimEnd())
        writer.execute {
            runCatching {
                val dir = directory ?: return@runCatching
                dir.mkdirs()
                val current = File(dir, "current.txt")
                if (current.length() + line.toByteArray().size > 256 * 1024) {
                    File(dir, "previous.txt").delete()
                    check(current.renameTo(File(dir, "previous.txt")))
                }
                current.appendText(line)
            }.onFailure { android.util.Log.w("SyncDiagnostics", "Diagnostic write failed: ${it.javaClass.simpleName}") }
        }
    }

    /** Called on a worker thread; queue ordering includes all previously submitted events. */
    fun snapshot(): String = writer.submit<String> {
        val dir = directory
        "School sync diagnostics (no page contents, credentials or student identifiers)\n" +
            if (dir == null) "Diagnostics unavailable\n" else
                listOf("previous.txt", "current.txt").joinToString("") { name ->
                    File(dir, name).let { if (it.exists()) it.readText() else "" }
                }
    }.get(10, java.util.concurrent.TimeUnit.SECONDS)
}

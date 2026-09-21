package com.tyust.course.utils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object LogUtils {
    fun exportLogs(context: Context) {
        Thread {
            val result = runCatching {
                val logFile = File(context.cacheDir, "logs/school-sync-${System.currentTimeMillis()}.txt")
                logFile.parentFile?.mkdirs()
                logFile.writeText(SyncDiagnostics.snapshot())
                if (android.os.Build.VERSION.SDK_INT >= 29) saveToDownloads(context, logFile)
                logFile
            }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { file ->
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        Toast.makeText(context, "日志已保存到 Download/CampusAssistant，可稍后分享或连接电脑读取。", Toast.LENGTH_LONG).show()
                    } else {
                        runCatching { shareFile(context, file) }.onFailure {
                            Toast.makeText(context, "无法打开日志分享，请稍后重试。", Toast.LENGTH_LONG).show()
                        }
                    }
                }.onFailure {
                    Toast.makeText(context, "日志导出失败，请稍后重试。", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    @androidx.annotation.RequiresApi(29)
    private fun saveToDownloads(context: Context, file: File) {
        val resolver = context.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/CampusAssistant")
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { output -> file.inputStream().use { it.copyTo(output) } }
            values.clear()
            values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            check(resolver.update(uri, values, null, null) == 1)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("同步诊断日志", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "导出日志"))
    }
}

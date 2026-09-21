package com.tyust.course.academic

import android.content.ContextWrapper
import com.tyust.course.utils.SyncDiagnostics
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SyncDiagnosticsTest {
    @Test fun historySurvivesReinitializationAndRotatesWithinLimit() {
        val root = Files.createTempDirectory("sync-diagnostics-test").toFile()
        val context = object : ContextWrapper(null) {
            override fun getNoBackupFilesDir(): File = root
        }
        try {
            SyncDiagnostics.initialize(context)
            SyncDiagnostics.record("before_restart")
            assertTrue(SyncDiagnostics.snapshot().contains("before_restart"))
            SyncDiagnostics.initialize(context)
            assertTrue(SyncDiagnostics.snapshot().contains("before_restart"))
            repeat(1000) { SyncDiagnostics.record("tick", "index=$it " + "x".repeat(650)) }
            SyncDiagnostics.record("latest_event")
            val snapshot = SyncDiagnostics.snapshot()
            assertTrue(snapshot.contains("latest_event"))
            assertFalse(snapshot.contains("before_restart"))
            val files = File(root, "sync-diagnostics").listFiles()!!
            assertEquals(2, files.size)
            assertTrue(files.sumOf { it.length() } <= 512 * 1024)
        } finally {
            root.deleteRecursively()
        }
    }
}

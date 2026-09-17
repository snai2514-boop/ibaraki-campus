package com.tyust.course.manager

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import java.io.File

object ThemePackManager {
    var active by mutableStateOf<ThemePack?>(null)
        private set
    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, "theme-pack.zip"))
    private fun validate(pack: ThemePack): ThemePack {
        (listOfNotNull(pack.image) + pack.frames).forEach { bytes ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            require(options.outWidth in 1..4096 && options.outHeight in 1..4096 && options.outWidth.toLong() * options.outHeight <= 8_000_000) { "背景须为不超过 4096 像素、800 万像素的图片" }
        }
        pack.frames.forEach { bytes ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            require(options.outWidth <= 1024 && options.outHeight <= 1024 && options.outMimeType == "image/png") { "启动动画每帧须为 1024 像素以内 PNG" }
        }
        return pack
    }
    fun initialize(context: Context) { active = runCatching { validate(ThemePackParser.parse(file(context).readFully())) }.getOrNull() }
    fun read(context: Context, uri: Uri): ThemePack = context.contentResolver.openInputStream(uri)!!.use {
        validate(ThemePackParser.parse(ThemePackParser.readLimited(it, ThemePackParser.MAX_ARCHIVE)))
    }
    fun apply(context: Context, pack: ThemePack) {
        val previousIcon = active?.appIcon ?: "default"
        switchIcon(context, pack.appIcon)
        val output = file(context)
        var stream: java.io.FileOutputStream? = null
        try { stream = output.startWrite(); stream.write(pack.archive); output.finishWrite(stream) } catch (e: Exception) { output.failWrite(stream); switchIcon(context, previousIcon); throw e }
        active = pack
    }
    fun clear(context: Context) { switchIcon(context, "default"); file(context).delete(); active = null }
    private fun switchIcon(context: Context, icon: String) {
        val pm = context.packageManager
        val names = mapOf("default" to "LauncherDefault", "sky" to "LauncherSky", "night" to "LauncherNight")
        fun set(name: String, enabled: Boolean) = pm.setComponentEnabledSetting(android.content.ComponentName(context.packageName, "com.tyust.course.$name"),
            if (enabled) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            android.content.pm.PackageManager.DONT_KILL_APP)
        val selected = names.getValue(icon)
        set(selected, true)
        names.values.filter { it != selected }.forEach { set(it, false) }
    }
    fun colors(base: ColorScheme, dark: Boolean): ColorScheme {
        val pack = active ?: return base
        val palette = if (dark) pack.dark else pack.light
        fun c(key: String) = Color(android.graphics.Color.parseColor(palette.getValue(key)))
        return base.copy(primary = c("primary"), onPrimary = c("onPrimary"), background = c("background"), surface = c("surface"),
            onBackground = c("text"), onSurface = c("text"), onSurfaceVariant = c("muted"), primaryContainer = c("accent"),
            onPrimaryContainer = c("text"), surfaceVariant = c("accent"), surfaceContainerHigh = c("dialog"))
    }
}

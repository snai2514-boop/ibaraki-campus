package com.tyust.course.manager

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.json.JSONObject

data class ThemePack(val name: String, val author: String, val light: Map<String, String>, val dark: Map<String, String>, val image: ByteArray?, val archive: ByteArray,
    val appIcon: String, val frames: List<ByteArray>, val frameDurationMs: Int)

/** Data-only ZIP format. No file is extracted using an archive-controlled path. */
object ThemePackParser {
    const val MAX_ARCHIVE = 2_000_000
    val colorKeys = setOf("primary", "onPrimary", "background", "surface", "text", "muted", "accent")
    fun readLimited(input: java.io.InputStream, maximum: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (output.size() <= maximum) {
            val count = input.read(buffer, 0, minOf(buffer.size, maximum + 1 - output.size()))
            if (count < 0) break
            if (count == 0) break
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
    fun parse(bytes: ByteArray): ThemePack {
        require(bytes.size in 22..MAX_ARCHIVE) { "美化包须为 2 MB 以内的 ZIP" }
        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require((entry.name in setOf("theme.json", "background.png", "background.jpg") || Regex("startup-[1-8]\\.png").matches(entry.name)) && !entry.isDirectory && entry.name !in files) { "包内包含不支持的文件或重复文件" }
                val max = if (entry.name == "theme.json") 16_384 else if (entry.name.startsWith("startup-")) 128_000 else 1_000_000
                val data = readLimited(zip, max)
                require(data.size <= max) { "主题配置或背景图片过大" }
                files[entry.name] = data
                require(files.values.sumOf { it.size } <= 2_000_000) { "解压后的美化包超过 2 MB" }
            }
        }
        val json = JSONObject(files["theme.json"]?.toString(Charsets.UTF_8) ?: error("缺少 theme.json"))
        require(json.optString("format") == "ibaraki-campus-theme" && json.opt("version") == 1) { "不支持的美化包格式或版本" }
        val name = json.getString("name").trim(); val author = json.optString("author").trim()
        require(name.length in 1..60 && author.length <= 80) { "名称或作者过长" }
        fun palette(key: String): Map<String, String> {
            val value = json.getJSONObject(key)
            val colors = colorKeys.associateWith { field -> value.getString(field).also {
                require(Regex("#[0-9a-fA-F]{6}").matches(it)) { "颜色必须为 #RRGGBB" }
            } }
            val dialog = value.optString("dialog", colors.getValue("surface"))
            require(Regex("#[0-9a-fA-F]{6}").matches(dialog)) { "弹窗颜色必须为 #RRGGBB" }
            return colors + ("dialog" to dialog)
        }
        val imageName = json.optString("background")
        require(imageName.isEmpty() || imageName in setOf("background.png", "background.jpg")) { "背景必须是包内 PNG 或 JPEG" }
        val icon = json.optString("appIcon", "default")
        require(icon in setOf("default", "sky", "night")) { "不支持的桌面图标名称" }
        val startup = json.optJSONObject("startup")
        val frameNames = startup?.getJSONArray("frames")?.let { a -> (0 until a.length()).map(a::getString) }.orEmpty()
        val rawDuration = startup?.opt("frameDurationMs") ?: 150
        require(rawDuration is Int) { "帧间隔必须为整数" }
        val duration = rawDuration
        require(startup == null || (frameNames.size in 1..8 && duration in 80..350 && frameNames.distinct().size == frameNames.size && frameNames.all { Regex("startup-[1-8]\\.png").matches(it) })) { "启动动画须为 1–8 张 PNG，帧间隔 80–350 毫秒" }
        val expected = setOf("theme.json") + (if (imageName.isEmpty()) emptySet() else setOf(imageName)) + frameNames
        require(files.keys == expected) { "图片缺失或包含多余文件" }
        return ThemePack(name, author, palette("light"), palette("dark"), files[imageName], bytes, icon, frameNames.map { files.getValue(it) }, duration)
    }
}

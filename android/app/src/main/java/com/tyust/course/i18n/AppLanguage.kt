package com.tyust.course.i18n

import android.content.Context
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow

/** Presentation-only translations. Stored school records and personal input stay unchanged. */
object AppLanguage {
    val current = MutableStateFlow("ja")
    private var context: Context? = null
    private var catalog: TranslationCatalog = TranslationCatalog(emptyMap())
    fun initialize(value: Context) {
        context = value.applicationContext
        val json = JSONObject(value.assets.open("languages.json").bufferedReader().use { it.readText() })
        catalog = TranslationCatalog(json.keys().asSequence().associateWith { key ->
            val row = json.getJSONObject(key)
            listOf("en", "ja", "ko").associateWith { row.getString(it) }
        })
        val prefs = value.getSharedPreferences("app-language", Context.MODE_PRIVATE)
        val existing = java.io.File(value.noBackupFilesDir, "school-student-profile-v1.json").exists() ||
            java.io.File(value.noBackupFilesDir, "school-student-profile-v1.json.bak").exists()
        current.value = LanguageDefaults.select(prefs.getString("language", null), existing)
        prefs.edit().putString("language", current.value).apply()
    }
    fun select(language: String) {
        require(language in setOf("zh", "en", "ja", "ko"))
        context?.getSharedPreferences("app-language", Context.MODE_PRIVATE)?.edit()?.putString("language", language)?.apply()
        current.value = language
    }
    fun text(value: String, language: String = current.value) = catalog.translate(value, language)
}

object LanguageDefaults {
    fun select(saved: String?, existingProfile: Boolean): String =
        saved?.takeIf { it in setOf("zh", "en", "ja", "ko") } ?: if (existingProfile) "zh" else "ja"
}

class TranslationCatalog(private val entries: Map<String, Map<String, String>>) {
    private val uiValueTemplates = setOf("核对范围：{0}", "上限：{0} {1}", "上述范围内已有数量（{0}）", "拟增加数量（{0}）", "{0}. {1} ▾", "{0}. {1}")
    private data class Template(val pattern: Regex, val values: Map<String, String>, val localizeValues: Boolean)
    private val templates = entries.filterKeys { it.contains("{0}") }.map { (key, values) ->
        val pieces = Regex("\\{\\d+\\}").split(key)
        Template(Regex(pieces.joinToString("(.*?)") { Regex.escape(it) }, RegexOption.DOT_MATCHES_ALL), values, key in uiValueTemplates)
    }.sortedByDescending { it.pattern.pattern.length }
    fun translate(value: String, language: String): String {
        if (language == "zh" || value.isEmpty()) return value
        entries[value]?.get(language)?.let { return it }
        if (value.contains('\n')) return value.split('\n').joinToString("\n") { translate(it, language) }
        for (template in templates) {
            val match = template.pattern.matchEntire(value) ?: continue
            val translated = template.values[language] ?: return value
            return Regex("\\{(\\d+)\\}").replace(translated) { token ->
                val argument = match.groupValues.getOrElse(token.groupValues[1].toInt() + 1) { token.value }
                if (template.localizeValues) entries[argument]?.get(language) ?: argument else argument
            }
        }
        // App status strings are assembled as separate sentences; never translate substrings
        // inside names, course titles, locations or other unknown data.
        if (value.contains('；')) return value.split('；').joinToString("; ") { translate(it, language) }
        val sentences = Regex("[^。]+。?").findAll(value).map { it.value }.toList()
        if (sentences.size > 1) {
            val converted = sentences.map { translate(it, language) }
            if (converted != sentences) return converted.joinToString(" ")
        }
        return value
    }
}

package com.tyust.course.academic
import com.tyust.course.manager.ThemePackParser
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
class ThemePackTest {
    private fun config(): JSONObject {
        val palette = JSONObject().apply { ThemePackParser.colorKeys.forEach { put(it, "#123456") } }
        return JSONObject().put("format", "ibaraki-campus-theme").put("version", 1).put("name", "Test").put("light", palette).put("dark", JSONObject(palette.toString()))
    }
    private fun zip(json: JSONObject, extra: Map<String, ByteArray> = emptyMap()): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> (mapOf("theme.json" to json.toString().toByteArray()) + extra).forEach { (n,b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
        return out.toByteArray()
    }
    @Test fun arbitraryPalettesAndIndependentDialog() {
        val c=config();c.getJSONObject("light").put("dialog", "#FF0077")
        val p=ThemePackParser.parse(zip(c))
        assertEquals("#FF0077",p.light["dialog"]);assertEquals("#123456",p.dark["dialog"])
        assertEquals("default",p.appIcon);assertTrue(p.frames.isEmpty())
    }
    @Test fun rejectTraversalExtraFilesAndBomb() {
        for ((n,b) in listOf("../theme.json" to byteArrayOf(1), "script.js" to byteArrayOf(1), "background.png" to ByteArray(1_000_001))) {
            assertThrows(IllegalArgumentException::class.java) { ThemePackParser.parse(zip(config(), mapOf(n to b))) }
        }
    }
    @Test fun validateStartupReferencesAndBundledIcons() {
        val c=config().put("appIcon","night").put("startup",JSONObject().put("frames",org.json.JSONArray(listOf("startup-1.png"))).put("frameDurationMs",100))
        assertEquals(1,ThemePackParser.parse(zip(c,mapOf("startup-1.png" to byteArrayOf(1)))).frames.size)
        assertThrows(IllegalArgumentException::class.java) { ThemePackParser.parse(zip(c)) }
        c.put("appIcon","https://example.com/icon.png")
        assertThrows(IllegalArgumentException::class.java) { ThemePackParser.parse(zip(c)) }
    }
}

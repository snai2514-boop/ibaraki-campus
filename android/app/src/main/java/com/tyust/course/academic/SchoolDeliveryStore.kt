package com.tyust.course.academic

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray

class SchoolDeliveryStore(context: Context, owner: String) {
    companion object { val revision = MutableStateFlow(0L) }
    private val prefs=context.getSharedPreferences("school-delivery-$owner",Context.MODE_PRIVATE)
    fun load(year: Int, code: String): String = prefs.getString("$year/$code", "").orEmpty()
    fun save(year: Int, rows: JSONArray) {
        val editor=prefs.edit()
        for(i in 0 until rows.length()) {
            val row=rows.getJSONObject(i)
            val code=row.getString("code")
            if(code.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*"))) editor.putString("$year/$code",row.optString("delivery"))
        }
        check(editor.commit())
        revision.value++
    }
}

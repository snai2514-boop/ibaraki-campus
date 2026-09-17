package com.tyust.course.academic

import android.content.Context
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow

data class RegistrationCourse(val id: String, val code: String, val name: String, val schedule: String,
    val credits: String, val status: String, val available: Boolean)
data class RegistrationSnapshot(val student: String, val scope: String, val message: String,
    val signature: String, val rows: List<RegistrationCourse>, val time: Long)
data class RegistrationSlot(val day: Int, val period: Int)

object SchoolRegistrationReading {
    /** A verified read-only timetable is a valid status result, never evidence of eligibility. */
    fun readOnly(text: String, student: String): RegistrationSnapshot? = runCatching {
        val identities=Regex("学生番号 \\| ([A-Za-z0-9]+)").findAll(text).map {it.groupValues[1]}.toSet()
        require(identities==setOf(student))
        val item=PortalImportParser.parse(text,allowEmptyTimetable=true)
        require(item.key.startsWith("timetable-"))
        val scope=item.key.removePrefix("timetable-")
        val rows=item.lessons.groupBy {it.description.substringBefore(' ')}.map { (code,lessons) ->
            val description=lessons.first().description
            RegistrationCourse("$scope/$code",code,description.substringAfter(' ').substringBeforeLast(' '),
                lessons.joinToString(" / ") {"${"月火水木金土日"[it.day-1]} ${it.period}"},
                description.substringAfterLast(' ').removeSuffix("単位"),"已登记",false)
        }
        RegistrationSnapshot(student,scope,"履修状况已读取；学校当前页面未列出可确认的登记选项。","read-only:$scope",rows,0)
    }.getOrNull()
    fun json(snapshot: RegistrationSnapshot): JSONObject = JSONObject().put("ready",true).put("student",snapshot.student)
        .put("scope",snapshot.scope).put("message",snapshot.message).put("signature",snapshot.signature)
        .put("rows",org.json.JSONArray().apply {snapshot.rows.forEach {r -> put(JSONObject().put("id",r.id).put("code",r.code)
            .put("name",r.name).put("schedule",r.schedule).put("credits",r.credits).put("status",r.status).put("available",false))}})
}

object RegistrationPolicy {
    fun slots(schedule: String): Set<RegistrationSlot> {
        val value=java.text.Normalizer.normalize(schedule,java.text.Normalizer.Form.NFKC)
        val pattern=Regex("([月火水木金土日](?:曜日|曜)?(?:\\s*[・、/]\\s*[月火水木金土日](?:曜日|曜)?)*)\\s*[第:：]*\\s*([1-5](?:\\s*[-〜～・、/,]\\s*[1-5])*)(?![0-9〜～-])")
        return pattern.findAll(value).flatMap {match ->
            val days=days(match.groupValues[1]); val raw=match.groupValues[2]
            val numbers=Regex("[1-5]").findAll(raw).map {it.value.toInt()}.toList()
            val periods=if(Regex("[-〜～]").containsMatchIn(raw) && numbers.size==2) (numbers.first()..numbers.last()).toList() else numbers
            days.flatMap {day -> periods.map {RegistrationSlot(day,it)}}.asSequence()
        }.toSet()
    }
    fun reconcile(selected: Set<String>, previous: RegistrationSnapshot?, current: RegistrationSnapshot): Set<String> {
        if(previous?.student!=current.student || previous.scope!=current.scope) return emptySet()
        return current.rows.filter {it.available && it.id in selected && previous.rows.find {old -> old.id==it.id}==it}.map {it.id}.toSet()
    }
    fun days(schedule: String): List<Int> {
        val value=schedule.replace("曜日","").replace("曜","")
        return "月火水木金土日".mapIndexedNotNull { index, day -> if(value.contains(day)) index + 1 else null }
    }
    fun fresh(time: Long, now: Long): Boolean = time > 0 && now >= time && now - time < 5 * 60_000
    fun newAvailable(old: RegistrationSnapshot?, current: RegistrationSnapshot): List<RegistrationCourse> {
        val before = old?.takeIf { it.student == current.student && it.scope == current.scope }?.rows.orEmpty().filter { it.available }.map { it.id }.toSet()
        return current.rows.filter { it.available && it.id !in before }
    }
}

class SchoolRegistrationStore(private val context: Context, private val student: String) {
    companion object { val revision = MutableStateFlow(0L) }
    private val key = java.security.MessageDigest.getInstance("SHA-256").digest(student.toByteArray()).joinToString("") { "%02x".format(it) }
    private val prefs = context.getSharedPreferences("registration-$key", Context.MODE_PRIVATE)
    fun load(): RegistrationSnapshot? = runCatching { decode(JSONObject(prefs.getString("snapshot", "")!!)) }.getOrNull()
    private fun decode(j: JSONObject): RegistrationSnapshot {
        require(j.getString("student") == student)
        val a=j.getJSONArray("rows")
        val rows=(0 until a.length()).map { i -> val r=a.getJSONObject(i)
            RegistrationCourse(r.getString("id"),r.getString("code"),r.getString("name"),r.optString("schedule"),r.optString("credits"),r.optString("status"),r.optBoolean("available")) }
        return RegistrationSnapshot(student,j.optString("scope"),j.optString("message"),j.getString("signature"),rows,j.optLong("time"))
    }
    fun save(json: JSONObject): RegistrationSnapshot {
        require(json.optBoolean("ready"))
        json.put("time",System.currentTimeMillis())
        val old=load(); val snapshot=decode(json)
        val fresh=RegistrationPolicy.newAvailable(old,snapshot)
        val editor=prefs.edit().putString("snapshot",json.toString())
        if(fresh.isNotEmpty()) editor.putString("alert",fresh.joinToString("\n") { "${it.name} · ${it.schedule.ifBlank { "时间待确认" }}" })
        if(snapshot.rows.none { it.available }) editor.remove("alert")
        check(editor.commit()) { "履修状况保存失败" }
        revision.value++
        if(fresh.isNotEmpty()) SchoolNoticeNotifier(context).send("发现 ${fresh.size} 门可登录课程：\n"+fresh.joinToString("\n") { it.name },483,"可登录课程")
        return snapshot
    }
    fun alert(): String = prefs.getString("alert", "").orEmpty()
    fun pending(): Boolean = prefs.getBoolean("pending",false)
    fun beginSubmission(ids: Set<String>, scope: String) {check(prefs.edit().putBoolean("pending",true).putStringSet("pendingIds",ids).putString("pendingScope",scope).commit())}
    fun pendingIds(): Set<String> = prefs.getStringSet("pendingIds",emptySet()).orEmpty().toSet()
    fun pendingScope(): String = prefs.getString("pendingScope","").orEmpty()
    fun markPending(value: Boolean) {check(prefs.edit().putBoolean("pending",value).commit())}
    fun acknowledge() { prefs.edit().remove("alert").apply(); revision.value++ }
}

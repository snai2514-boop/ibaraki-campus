package com.tyust.course.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyust.course.academic.*
import com.tyust.course.i18n.LocalizedText as Text

/** Native selection state lives in the activity, independently of dialogs and the school DOM. */
@Composable
fun RegistrationWeekScreen(snapshot: RegistrationSnapshot?, selected: Set<String>, status: String,
    enabled: Boolean, busy: Boolean, pending: Boolean, onToggle: (RegistrationCourse, Boolean) -> Unit,
    onRead: () -> Unit, onSubmit: () -> Unit, onBack: () -> Unit, onResultChecked: () -> Unit,
    onLogin: () -> Unit, onSchoolWeb: () -> Unit, lastResult: String = "") {
    var choosing by remember { mutableStateOf<List<String>?>(null) }
    var selectionTitle by remember { mutableStateOf("") }
    var info by remember { mutableStateOf(false) }
    var showingResult by remember { mutableStateOf(false) }
    var previousResult by remember { mutableStateOf(lastResult) }
    LaunchedEffect(lastResult,busy) {
        if(!busy && lastResult.isNotBlank() && lastResult!=previousResult) {
            choosing=null;showingResult=true;info=true
        }
        if(!busy) previousResult=lastResult
    }
    val rows=snapshot?.rows.orEmpty().filter {it.available}
    val native=snapshot?.signature?.startsWith("native:")==true
    val slots=remember(rows) {rows.associate {it.id to RegistrationPolicy.slots(it.schedule)}}
    val line=MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick=onBack) {Text("返回")}
            Text("可登录课程",Modifier.weight(1f),fontSize=22.sp,fontWeight=FontWeight.Bold)
            TextButton(onClick=onRead,enabled=!busy && !pending) {Text("刷新")}
        }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(status.isNotBlank()) TextButton(onClick={showingResult=false;info=true},modifier=Modifier.fillMaxWidth()) {
            Text(status,maxLines=2,overflow=TextOverflow.Ellipsis,fontSize=12.sp)
        }
        if(native) Text("可跨时间格多选课程，确认一次，全部提交后自动统一查询结果。",fontSize=12.sp,modifier=Modifier.padding(vertical=4.dp))
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            Spacer(Modifier.width(40.dp))
            (1..5).forEach {Text("星期${"一二三四五"[it-1]}",Modifier.weight(1f).padding(vertical=12.dp),textAlign=TextAlign.Center,fontSize=13.sp)}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            IbarakiTimetable.periodTimes.forEach { (period,time) ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Column(Modifier.width(40.dp).heightIn(min=112.dp).padding(top=12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(period.toString(),fontWeight=FontWeight.Bold,fontSize=19.sp)
                        Text(time.first,fontSize=9.sp); Text(time.second,fontSize=9.sp)
                    }
                    (1..5).forEach { day ->
                        val entries=rows.filter {RegistrationSlot(day,period) in slots[it.id].orEmpty()}
                        Column(Modifier.weight(1f).fillMaxHeight().heightIn(min=112.dp).border(.5.dp,line).padding(2.dp)) {
                            if(entries.size==1) {
                                val course=entries.single()
                                Card(onClick={choosing=listOf(course.id);selectionTitle=course.name},modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    Checkbox(checked=course.id in selected,onCheckedChange={onToggle(course,it)},enabled=enabled && !busy && !pending)
                                    Text(course.name,fontSize=12.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
                                    if(course.faculty.isNotBlank()) Text(course.faculty,fontSize=10.sp,textAlign=TextAlign.Center,maxLines=2,overflow=TextOverflow.Ellipsis)
                                    if(course.remote in listOf("○","〇","◯","有","あり")) Text("线上",fontSize=10.sp)
                                } }
                            } else if(entries.size>1) {
                                Card(onClick={choosing=entries.map {it.id};selectionTitle="星期${"一二三四五"[day-1]} · 第 $period 限"},modifier=Modifier.fillMaxWidth()) {
                                    Column(Modifier.fillMaxWidth().padding(vertical=16.dp,horizontal=3.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                        Text("${entries.size} 门课程",fontSize=13.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                                        val chosen=entries.count {it.id in selected}
                                        if(chosen>0) Text("已选 $chosen 门",fontSize=11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val unplaced=rows.filter {slots[it.id].orEmpty().none {slot -> slot.day in 1..5 && slot.period in IbarakiTimetable.periodTimes}}
        if(unplaced.isNotEmpty()) TextButton(onClick={choosing=unplaced.map {it.id};selectionTitle="周历外／时间待确认"}) {Text("周历外／时间待确认：${unplaced.size} 门")}
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("已选 ${selected.size} 门",Modifier.weight(1f),fontSize=13.sp)
            FilledTonalButton(onClick=onSubmit,enabled=enabled && !busy && !pending && selected.isNotEmpty()) {Text("登录已选 ${selected.size} 门")}
        }
        if(pending) TextButton(onClick=onResultChecked,enabled=!busy) {Text("重新查询登记结果")}
        if(lastResult.isNotBlank()) TextButton(onClick={showingResult=true;info=true}) {Text("查看登记结果")}
    }
    choosing?.let { ids -> AlertDialog(onDismissRequest={choosing=null},title={Text(selectionTitle)},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
            val choices=rows.filter {it.id in ids}
            if(choices.isEmpty()) Text("课程状态已变化，请刷新。")
            choices.forEach { course -> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=course.id in selected,onCheckedChange={onToggle(course,it)},enabled=enabled && !busy && !pending)
                Column(Modifier.weight(1f).padding(vertical=8.dp)) {
                    Text(course.name,fontWeight=FontWeight.SemiBold)
                    if(course.faculty.isNotBlank()) Text("开课学部：${course.faculty}",fontSize=12.sp)
                    Text(course.code + if(course.credits.isNotBlank()) " · ${course.credits} 学分" else "",fontSize=12.sp)
                    Text(course.schedule,fontSize=12.sp)
                    if(course.teacher.isNotBlank()) Text(course.teacher,fontSize=12.sp)
                    if(course.remote.isNotBlank()) Text("学校远隔授课标记：${course.remote}",fontSize=12.sp)
                }
            } }
        }
    },confirmButton={TextButton(onClick={choosing=null}) {Text("完成")}}) }
    if(info) AlertDialog(onDismissRequest={info=false},title={Text(if(showingResult) "登记结果" else "履修状况")},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {Text(if(showingResult) lastResult else status)}
    },
        confirmButton={TextButton(onClick={info=false}) {Text("关闭")}},
        dismissButton={Row {
            TextButton(onClick={info=false;onSchoolWeb()}) {Text("查看学校原页")}
            if(!enabled && !pending) TextButton(onClick={info=false;onLogin()}) {Text("重新登录")}
        }})
}

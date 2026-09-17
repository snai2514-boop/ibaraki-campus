package com.tyust.course.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.tyust.course.academic.CreditProgress
import com.tyust.course.i18n.LocalizedText as Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** Refresh on resume and midnight; the selected semester is never restored from an old screen. */
@Composable
internal fun rememberPhoneDate(): String {
    fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    var date by remember { mutableStateOf(today()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) date = today()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { while (true) { date = today(); delay(30_000) } }
    return date
}

@Composable
internal fun ForecastBar(earned: Float, added: Float, total: Float, tint: Color, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        // Optional subcategories have no individual minimum; still show their credits.
        // Expand the scale for overflow so additional credits never disappear at 100%.
        val scale = maxOf(total, earned + added, 1f)
        val start = (earned / scale).coerceIn(0f, 1f) * size.width
        val end = ((earned + added) / scale).coerceIn(0f, 1f) * size.width
        drawRoundRect(track, cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
        if(start > 0) drawRect(tint, size = Size(start, size.height))
        if(end > start) {
            val violet = Color(0xFF9781EE)
            drawRect(violet.copy(alpha = .18f), topLeft = Offset(start, 0f), size = Size(end-start, size.height))
            drawRect(violet, topLeft = Offset(start, 1f), size = Size(end-start, size.height-2f), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
        }
    }
}

@Composable
internal fun ForecastRing(earned: Float, added: Float, total: Float, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val stroke = 12.dp.toPx()
        val origin = Offset(stroke/2, stroke/2)
        val area = Size(size.width-stroke, size.height-stroke)
        val actual = if(total > 0) (earned / total).coerceIn(0f,1f) else 0f
        val future = if(total > 0) ((earned+added)/total).coerceIn(actual,1f)-actual else 0f
        drawArc(track, -90f, 360f, false, origin, area, style=Stroke(stroke))
        drawArc(Color(0xFF2DBDF0), -90f, actual*360, false, origin, area, style=Stroke(stroke))
        if(future > 0) drawArc(Color(0xFF9781EE), -90f+actual*360, future*360, false, origin, area,
            style=Stroke(stroke, pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))))
    }
}

@Composable
internal fun ForecastBranch(actual: CreditProgress, projected: CreditProgress?) {
    Column(Modifier.fillMaxWidth().padding(start=8.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("${actual.requirement.label}  ${actual.display}", style=MaterialTheme.typography.bodyMedium)
        ForecastBar(actual.earned.toFloat(), projected?.earned?.toFloat() ?: 0f, actual.requirement.required.toFloat(), Color(0xFF6FAAFF))
        Text("预计新增 +${projected?.earned?.stripTrailingZeros()?.toPlainString() ?: "0"}", style=MaterialTheme.typography.labelSmall)
        actual.children.forEach { child -> ForecastBranch(child, projected?.children?.find { it.requirement.id == child.requirement.id }) }
    }
}

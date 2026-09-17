package com.tyust.course.i18n

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.TextUnit

@Composable
fun LocalizedText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontStyle: FontStyle? = null, fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null, textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true, maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1, onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current) {
    val language by AppLanguage.current.collectAsState()
    Text(text = AppLanguage.text(text, language), modifier = modifier, color = color,
        fontSize = fontSize, fontStyle = fontStyle, fontWeight = fontWeight, fontFamily = fontFamily,
        letterSpacing = letterSpacing, textDecoration = textDecoration, textAlign = textAlign,
        lineHeight = lineHeight, overflow = overflow, softWrap = softWrap, maxLines = maxLines,
        minLines = minLines, onTextLayout = onTextLayout, style = style)
}

@Composable
fun LanguageButton() {
    var open by remember { mutableStateOf(false) }
    val language by AppLanguage.current.collectAsState()
    IconButton(onClick = { open = true }) {
        Icon(Icons.Outlined.Language, contentDescription = AppLanguage.text("切换语言", language))
    }
    if (open) AlertDialog(onDismissRequest = { open = false },
        title = { LocalizedText("语言") },
        text = { androidx.compose.foundation.layout.Column {
            listOf("zh" to "中文", "en" to "English", "ko" to "한국어", "ja" to "日本語").forEach { (code, label) ->
                TextButton(onClick = { AppLanguage.select(code); open = false }) { Text((if (language == code) "✓ " else "") + label) }
            }
        } }, confirmButton = { TextButton(onClick = { open = false }) { LocalizedText("关闭") } })
}

@Composable
fun SupportFooter(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.foundation.layout.Column(modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        LocalizedText("如有异常、Bug 或建议，请发送邮件反馈。", style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center)
        TextButton(onClick = {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO,
                android.net.Uri.parse("mailto:snai2514@gmail.com"))
            runCatching { context.startActivity(intent) }.onFailure {
                android.widget.Toast.makeText(context, AppLanguage.text("请使用邮件应用联系 snai2514@gmail.com"), android.widget.Toast.LENGTH_LONG).show()
            }
        }) { Text("snai2514@gmail.com", style = MaterialTheme.typography.labelMedium) }
    }
}

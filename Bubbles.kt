package com.didi.chatarchive.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.didi.chatarchive.model.ChatMessage
import com.didi.chatarchive.model.Kind

fun timeLabel(m: ChatMessage): String {
    val h12 = if (m.hour % 12 == 0) 12 else m.hour % 12
    return "$h12:${m.minute.toString().padStart(2, '0')} ${if (m.hour < 12) "am" else "pm"}"
}

private val FORMAT = Regex("""(?<![\p{L}\p{N}])(?:\*([^*\n]+)\*|_([^_\n]+)_|~([^~\n]+)~)(?![\p{L}\p{N}])""")

/** WhatsApp-style *bold*, _italic_, ~strike~ — the characters are only hidden when a valid pair exists. */
fun formatWhatsApp(text: String): AnnotatedString {
    if ('*' !in text && '_' !in text && '~' !in text) return AnnotatedString(text)
    return buildAnnotatedString {
        var last = 0
        for (m in FORMAT.findAll(text)) {
            append(text.substring(last, m.range.first))
            val b = m.groups[1]?.value; val i = m.groups[2]?.value; val s = m.groups[3]?.value
            when {
                b != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(b) }
                i != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(i) }
                s != null -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(s) }
            }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }
}

@Composable
fun DateChip(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(color = Color.White.copy(alpha = 0.92f), shape = RoundedCornerShape(10.dp), shadowElevation = 0.5.dp) {
            Text(label, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 12.sp, color = Pink.Muted, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SystemNotice(text: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        Surface(color = Pink.Chip, shape = RoundedCornerShape(10.dp), shadowElevation = 0.5.dp) {
            Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = Pink.Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MessageBubble(m: ChatMessage, firstInGroup: Boolean, maxBubbleWidth: Dp, highlighted: Boolean = false) {
    val out = m.outgoing
    val shape = RoundedCornerShape(
        topStart = if (!out && firstInGroup) 2.dp else 12.dp,
        topEnd = if (out && firstInGroup) 2.dp else 12.dp,
        bottomEnd = 12.dp, bottomStart = 12.dp
    )
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = if (firstInGroup) 6.dp else 1.5.dp),
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (out) Pink.Outgoing else Pink.Incoming,
            shape = shape, shadowElevation = 0.6.dp,
            border = if (highlighted) BorderStroke(2.dp, Pink.Primary) else null,
            modifier = Modifier.widthIn(max = maxBubbleWidth)
        ) {
            Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 4.dp)) {
                BubbleContent(m)
                Text(
                    (if (m.edited) "edited  " else "") + timeLabel(m),
                    Modifier.align(Alignment.End).padding(top = 2.dp),
                    fontSize = 11.sp, color = Pink.Muted
                )
            }
        }
    }
}

@Composable
private fun BubbleContent(m: ChatMessage) {
    val body = 16.sp
    when (m.kind) {
        Kind.TEXT -> {
            val text = remember(m.text) { formatWhatsApp(m.text) }
            Text(text, fontSize = body, lineHeight = 22.sp, color = Pink.Ink)
        }
        Kind.MEDIA_OMITTED -> Text("Media omitted", fontSize = 15.sp, fontStyle = FontStyle.Italic, color = Pink.Muted)
        Kind.EMPTY -> Text("No content in the export", fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Pink.Muted)
        Kind.ATTACHMENT -> Text("Attachment: ${m.text}", fontSize = 15.sp, color = Pink.Ink)
        Kind.CONTACT -> {
            val parts = m.extra.split('\n')
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(min = 190.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Pink.Soft), contentAlignment = Alignment.Center) {
                    Text(parts[0].firstOrNull { it.isLetter() }?.uppercase() ?: "#", color = Pink.DeepRose, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(parts[0], fontSize = body, fontWeight = FontWeight.SemiBold, color = Pink.Ink)
                    if (parts.size > 1 && parts[1].isNotBlank()) Text(parts[1], fontSize = 13.sp, color = Pink.Muted)
                    Text("Contact card", fontSize = 11.sp, color = Pink.Muted)
                }
            }
        }
        Kind.LOCATION -> {
            val uri = LocalUriHandler.current
            Column {
                Text("Location", fontSize = body, fontWeight = FontWeight.SemiBold, color = Pink.Ink)
                Text(
                    m.text, fontSize = 14.sp, color = Pink.DeepRose, textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable_safe { runCatching { uri.openUri(m.text) } }
                )
            }
        }
        Kind.POLL -> Column(Modifier.widthIn(min = 200.dp)) {
            Text("Poll", fontSize = 12.sp, color = Pink.Muted, fontWeight = FontWeight.Medium)
            Text(m.text, fontSize = body, fontWeight = FontWeight.SemiBold, color = Pink.Ink, modifier = Modifier.padding(vertical = 4.dp))
            for (line in m.extra.split('\n').filter { it.isNotBlank() }) {
                val (label, votes) = line.split('\t').let { it[0] to it.getOrElse(1) { "0" } }
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(Pink.Soft))
                    Spacer(Modifier.width(8.dp))
                    Text(label, Modifier.weight(1f), fontSize = 15.sp, color = Pink.Ink)
                    Text("$votes", fontSize = 13.sp, color = Pink.Muted)
                }
            }
        }
        Kind.SYSTEM -> Text(m.text, fontSize = 13.sp, color = Pink.Muted)
    }
}

private fun Modifier.clickable_safe(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

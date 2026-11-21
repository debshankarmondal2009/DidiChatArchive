package com.didi.chatarchive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.didi.chatarchive.model.Kind
import com.didi.chatarchive.model.Participants
import com.didi.chatarchive.model.ParseResult

@Composable
fun WorkingScreen(label: String) {
    Column(
        Modifier.fillMaxSize().background(Pink.LightBg),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Pink.Primary)
        Spacer(Modifier.height(16.dp))
        Text(label, color = Pink.Muted)
    }
}

@Composable
fun ImportScreen(error: String?, canGoBack: Boolean, onPick: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Pink.LightBg).statusBarsPadding().navigationBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Chat Archive", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Pink.DeepRose)
        Spacer(Modifier.height(12.dp))
        Text(
            "Rebuild your conversation with ${Participants.CONTACT_DISPLAY_NAME} from the WhatsApp export (.zip or .txt). " +
                "Everything stays on this phone — the app has no internet access.",
            textAlign = TextAlign.Center, color = Pink.Ink, lineHeight = 22.sp
        )
        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(error, color = Color(0xFFB00020), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPick, colors = ButtonDefaults.buttonColors(containerColor = Pink.Primary)) { Text("Choose export file") }
        if (canGoBack) TextButton(onClick = onBack) { Text("Back to chat", color = Pink.DeepRose) }
    }
}

@Composable
fun PreviewScreen(result: ParseResult, sourceName: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val s = result.stats
    Column(Modifier.fillMaxSize().background(Pink.LightBg).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Check before opening", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Pink.DeepRose)
            Text(sourceName, color = Pink.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))

            SectionCard("Who is on which side (Didi's phone)") {
                Line("${Participants.OWNER_EXPORT_NAME}  →  RIGHT, pink bubbles (device owner)")
                Line("${Participants.CONTACT_SHORT_NAME}  →  LEFT, white bubbles")
                Line("Header: ${Participants.CONTACT_DISPLAY_NAME}")
            }
            Spacer(Modifier.height(12.dp))
            SectionCard("What will be shown") {
                Line("${s.kept} items · ${s.firstDate} → ${s.lastDate}")
                Line("Didi ${s.fromDidi} · Debshankar ${s.fromDebshankar} · notices ${s.system}")
                Line("Deleted messages removed: ${s.deletedDropped}")
                Line("Edited: ${s.edited} · Media placeholders: ${s.mediaOmitted}")
                Line("Contact cards ${s.contactCards} · locations ${s.locations} · polls ${s.polls}")
                if (s.empty > 0) Line("Entries with no content in the export: ${s.empty}")
                if (s.outOfOrderSteps > 0) Line("Original order kept; ${s.outOfOrderSteps} places step back by a minute or more in the export.")
                if (s.extraFilesInZip > 0) Line("${s.extraFilesInZip} media file(s) found in the ZIP; this version shows only text and placeholders.")
            }
            Spacer(Modifier.height(12.dp))
            Text("First messages", fontWeight = FontWeight.SemiBold, color = Pink.Ink)
            Spacer(Modifier.height(6.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().clip8().background(Pink.ChatBg).padding(vertical = 8.dp)) {
                val sample = result.messages.filter { it.kind != Kind.SYSTEM }.take(8)
                Column {
                    sample.forEachIndexed { i, m ->
                        MessageBubble(m, firstInGroup = i == 0 || sample[i - 1].outgoing != m.outgoing, maxBubbleWidth = maxWidth * 0.82f)
                    }
                }
            }
        }
        Surface(shadowElevation = 8.dp, color = Color.White) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Pink.Primary)) { Text("Looks right") }
            }
        }
    }
}

private fun Modifier.clip8() = this.then(Modifier.clip(RoundedCornerShape(12.dp)))

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Pink.DeepRose, modifier = Modifier.padding(bottom = 6.dp))
            content()
        }
    }
}

@Composable
private fun Line(text: String) = Text(text, fontSize = 14.sp, color = Pink.Ink, lineHeight = 21.sp)

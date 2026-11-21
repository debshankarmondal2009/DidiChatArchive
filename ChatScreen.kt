package com.didi.chatarchive.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.didi.chatarchive.data.ChatData
import com.didi.chatarchive.data.DateItem
import com.didi.chatarchive.data.MessageItem
import com.didi.chatarchive.model.Kind
import com.didi.chatarchive.model.Participants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatScreen(chat: ChatData, onImportAgain: () -> Unit) {
    val list = chat.items
    val last = list.lastIndex
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focus = remember { FocusRequester() }

    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf(emptyList<Int>()) }
    var matchPos by remember { mutableIntStateOf(0) }
    var showDates by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    // The list uses reverseLayout, so the chat opens at the newest message. list[last - r] is row r.
    fun scrollTo(index: Int, anchorTop: Boolean) {
        scope.launch {
            val h = listState.layoutInfo.viewportSize.height
            listState.scrollToItem(last - index, if (anchorTop) -(h - 160) else -(h / 2))
        }
    }

    BackHandler(enabled = searching) { searching = false; query = "" }

    LaunchedEffect(searching) { if (searching) focus.requestFocus() }
    LaunchedEffect(query, searching) {
        val q = query.trim()
        if (!searching || q.length < 2) { matches = emptyList(); return@LaunchedEffect }
        delay(250)
        val found = withContext(Dispatchers.Default) {
            list.indices.filter { i ->
                val m = (list[i] as? MessageItem)?.message
                m != null && m.kind != Kind.SYSTEM && m.text.contains(q, ignoreCase = true)
            }
        }
        matches = found
        matchPos = found.lastIndex
        if (found.isNotEmpty()) scrollTo(found.last(), false)
    }
    val currentMatch = if (searching) matches.getOrNull(matchPos) else null

    Column(Modifier.fillMaxSize().background(Pink.ChatBg).imePadding()) {
        // ---------- Top bar ----------
        Surface(color = Pink.DeepRose, shadowElevation = 4.dp) {
            Row(
                Modifier.statusBarsPadding().fillMaxWidth().height(60.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (searching) {
                    IconButton(onClick = { searching = false; query = "" }) { Icon(BackIcon, "Close search", tint = Color.White) }
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Search messages", color = Color.White.copy(alpha = 0.65f), fontSize = 17.sp)
                        BasicTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                        )
                    }
                    if (query.trim().length >= 2) {
                        Text(
                            if (matches.isEmpty()) "0" else "${matchPos + 1}/${matches.size}",
                            color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    IconButton(enabled = matches.isNotEmpty(), onClick = {
                        matchPos = (matchPos - 1 + matches.size) % matches.size; scrollTo(matches[matchPos], false)
                    }) { Icon(ArrowUpIcon, "Previous match", tint = Color.White) }
                    IconButton(enabled = matches.isNotEmpty(), onClick = {
                        matchPos = (matchPos + 1) % matches.size; scrollTo(matches[matchPos], false)
                    }) { Icon(ArrowDownIcon, "Next match", tint = Color.White) }
                } else {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Pink.Soft), contentAlignment = Alignment.Center) {
                        Text("D", color = Pink.DeepRose, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Participants.CONTACT_DISPLAY_NAME, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Archived chat · ${chat.rangeLabel}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Visual only: calling is not part of an offline archive.
                    val notAvailable = { Toast.makeText(context, "Calls aren't available in this archive", Toast.LENGTH_SHORT).show() }
                    IconButton(onClick = notAvailable) { Icon(VideoIcon, "Video call (not available)", tint = Color.White.copy(alpha = 0.85f)) }
                    IconButton(onClick = notAvailable) { Icon(CallIcon, "Voice call (not available)", tint = Color.White.copy(alpha = 0.85f)) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(MoreIcon, "More", tint = Color.White) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Search") }, onClick = { menuOpen = false; searching = true })
                            DropdownMenuItem(text = { Text("Jump to date") }, onClick = { menuOpen = false; showDates = true })
                            DropdownMenuItem(text = { Text("Import again") }, onClick = { menuOpen = false; onImportAgain() })
                        }
                    }
                }
            }
        }

        // ---------- Conversation ----------
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val maxBubble = maxWidth * 0.82f
            LazyColumn(
                state = listState, reverseLayout = true, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(count = list.size, key = { list[last - it].id }) { row ->
                    val index = last - row
                    when (val item = list[index]) {
                        is DateItem -> DateChip(item.label)
                        is MessageItem ->
                            if (item.message.kind == Kind.SYSTEM) SystemNotice(item.message.text)
                            else MessageBubble(item.message, item.firstInGroup, maxBubble, highlighted = index == currentMatch)
                    }
                }
            }
            val showFab by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
            if (showFab) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (listState.firstVisibleItemIndex > 300) listState.scrollToItem(0) else listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    containerColor = Color.White, contentColor = Pink.DeepRose
                ) { Icon(ArrowDownIcon, "Scroll to latest message") }
            }
        }

        // ---------- Read-only footer (this is an archive; nothing can be sent) ----------
        Surface(color = Pink.Soft) {
            Text(
                "Offline archive · read only",
                Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 14.dp),
                textAlign = TextAlign.Center, color = Pink.Muted, fontSize = 13.sp
            )
        }
    }

    if (showDates) {
        AlertDialog(
            onDismissRequest = { showDates = false },
            title = { Text("Jump to date") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(chat.dates.asReversed()) { d ->
                        Row(
                            Modifier.fillMaxWidth().clickable { showDates = false; scrollTo(d.itemIndex, true) }.padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(d.label, color = Pink.Ink)
                            Text("${d.count}", color = Pink.Muted, fontSize = 13.sp)
                        }
                        HorizontalDivider(color = Pink.Soft)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDates = false }) { Text("Close") } }
        )
    }
}
